package com.summer.devopsplatform.terraform.service;

import com.summer.devopsplatform.terraform.dto.TerraformResult;
import com.summer.devopsplatform.terraform.entity.MachineRequest;
import com.summer.devopsplatform.terraform.repository.MachineRequestRepository;
import lombok.Data;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DaemonExecutor;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.PumpStreamHandler;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;

@Service
public class TerraformService {
    private static final String TF_DIRECORY = "G:\\terraform_work";
    private final MachineRequestRepository machineRequestRepository;

    // 通过构造函数注入Repository
    public TerraformService(MachineRequestRepository machineRequestRepository) {
        this.machineRequestRepository = machineRequestRepository;
    }
    public TerraformResult createMachine(String environment,String instanceType,String awsRegion){
        // 1. 创建并保存请求记录到数据库
        MachineRequest machineRequest = new MachineRequest(environment, instanceType, awsRegion);
        machineRequest = machineRequestRepository.save(machineRequest);

        try {
            //1.动态生成terraform的配置文件
            generateTerrafromConfig(environment,instanceType,awsRegion);
            //2.执行terraform的init
            TerraformResult initResult = execteTerraform("init");
            if (!initResult.isSuccess()){
                return initResult;
            }


            //3.执行terraform的apply
            TerraformResult applyResult = execteTerraform("apply","-auto-approve");

            // 5. 根据执行结果更新数据库
            if (applyResult.isSuccess()) {
                updateMachineRequestSuccess(machineRequest.getId(), applyResult);
            } else {
                updateMachineRequestFailure(machineRequest.getId(), applyResult.getMessage());
            }


            return applyResult;
        }catch (Exception e){
            return new TerraformResult(false,"创建机器的时候发生错误" + e.getMessage());
        }

    }

    // 更新成功状态的方法
    private void updateMachineRequestSuccess(Long requestId, TerraformResult result) {
        MachineRequest machineRequest = machineRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("请求记录不存在"));

        machineRequest.setSuccess(true);
        machineRequest.setMessage(result.getMessage());
        machineRequest.setInstanceId(result.getInstanceId());
        machineRequest.setPublicIp(result.getPublicip());
        machineRequest.setCompletionTime(LocalDateTime.now());

        machineRequestRepository.save(machineRequest);
    }

    // 更新失败状态的方法
    private void updateMachineRequestFailure(Long requestId, String errorMessage) {
        MachineRequest machineRequest = machineRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("请求记录不存在"));

        machineRequest.setSuccess(false);
        machineRequest.setMessage(errorMessage);
        machineRequest.setCompletionTime(LocalDateTime.now());

        machineRequestRepository.save(machineRequest);
    }
    private void generateTerrafromConfig(String environment,String instanceType,String awsRegion) throws IOException{
        //TF的配置模板
        String tfConfig = String.format(
                "provider \"aws\" {\n" +
                        "  region = \"%s\"\n" +
                        "}\n\n" +
                        "resource \"aws_instance\" \"app_server\" {\n" +
                        "  ami           = \"ami-0c55b159cbfafe1f0\" # 请根据你的区域更换AMI\n" +
                        "  instance_type = \"%s\"\n\n" +
                        "  tags = {\n" +
                        "    Name = \"%s-AppServer\"\n" +
                        "    Environment = \"%s\"\n" +
                        "    ManagedBy = \"YourDevOpsPlatform\"\n" +
                        "  }\n" +
                        "}\n\n" +
                        "output \"instance_id\" {\n" +
                        "  value = aws_instance.app_server.id\n" +
                        "}\n\n" +
                        "output \"public_ip\" {\n" +
                        "  value = aws_instance.app_server.public_ip\n" +
                        "}", awsRegion, instanceType, environment, environment);
        Files.write(
                Paths.get(TF_DIRECORY + "main.tf"),
                tfConfig.getBytes(StandardCharsets.UTF_8) // 明确指定使用 UTF-8 编码进行转换
        );

    }
    private TerraformResult execteTerraform(String... commands)throws IOException{
        //准备两个桶，一个装正常输出。一个装错误输出
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ByteArrayOutputStream errorStream = new ByteArrayOutputStream();
        //2.组装要执行的命令
        CommandLine cmdline = new CommandLine("terraform");
        for (String arg:commands){
            cmdline.addArgument(arg);
        }
        //设置执行任务
        DefaultExecutor executor = new DaemonExecutor();
        executor.setWorkingDirectory(new File(TF_DIRECORY)); //告诉执行任务的地方
        //让执行完成把执行的结果。根据正确和错误放在两个桶中
        executor.setStreamHandler(new PumpStreamHandler(outputStream,errorStream));
        //派机器人执行命令
        try {
            int exitCode = executor.execute(cmdline);
            String output = outputStream.toString();

            //执行命令
            if (exitCode == 0){
                String instanceId = parseOutput(output, "instance_id");
                String publicIp = parseOutput(output, "public_ip");
                TerraformResult result = new TerraformResult(true,"命令执行成功" + output);
                result.setInstanceId(instanceId);
                result.setPublicip(publicIp);
                return result;
            }else {
                return new TerraformResult(false,"命令执行失败" + output);
            }
        }catch (IOException e){
            return new TerraformResult(false,"执行命令的时候发生IO异常" + e.getMessage());
        }



    }
    private String parseOutput(String output, String key) {
        String[] lines = output.split("\\r?\\n"); // 把输出按行切分
        for (String line : lines) {
            if (line.contains(key)) { // 找到包含我们想要的关键词的那一行
                // 例如：找到 "instance_id = i-123456"
                // 用等号“=”分割，取右边部分，去掉空格和引号
                return line.split("=")[1].trim().replaceAll("\"", "");
            }
        }
        return null; // 没找到就返回空
    }
}
