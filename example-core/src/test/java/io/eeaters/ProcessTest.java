package io.eeaters;


import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Unit test for simple App.
 */
public class ProcessTest {


    @Test
    public void processTest() throws IOException, InterruptedException {
        Process process = Runtime.getRuntime().exec("pwd");

        // 获取命令输出
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("====" + line);
            }
        }
        int exitCode = process.waitFor();
        System.out.println("进程退出码：" + exitCode);
    }

    @Test
    public void processBuilderTest() throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder("java", "--version");
        Process start = processBuilder.start();
        // 获取命令输出
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(start.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("====" + line);
            }
        }
        int exitCode = start.waitFor();
        System.out.println("进程退出码：" + exitCode);
    }



}
