package top.mqk233.ddns.schedule;

import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.alidns.model.v20150109.AddDomainRecordRequest;
import com.aliyuncs.alidns.model.v20150109.DescribeDomainRecordsRequest;
import com.aliyuncs.alidns.model.v20150109.DescribeDomainRecordsResponse;
import com.aliyuncs.alidns.model.v20150109.UpdateDomainRecordRequest;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.profile.DefaultProfile;
import com.google.common.net.InternetDomainName;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

/**
 * 动态域名解析定时器
 *
 * @author mqk233
 * @since 2021-02-09
 */
@Configuration
public class DynamicDNSSchedules {
    @Resource
    private ApplicationArguments arguments;

    @Value("${aliyun.access-key}")
    private String aliyunAccessKey;

    @Value("${aliyun.secret-key}")
    private String aliyunSecretKey;

    @Scheduled(fixedDelayString = "${aliyun.update-interval}")
    public void aliyunDynamicDNS() {
        // 校验输入参数
        if (arguments.getSourceArgs() == null || arguments.getSourceArgs().length != 1) {
            System.out.println("参数输入错误，请输入本机地址需要指向的域名");
            return;
        }
        // 用户输入的域名
        String originDomain = arguments.getSourceArgs()[0];
        // 主域名
        String domain = String.valueOf(InternetDomainName.from(originDomain).topPrivateDomain());
        // 子域名
        String subdomain = domain.equals(originDomain) ? "@" : originDomain.replace("." + originDomain, "");

        // 获取本机地址
        StringBuilder responseResult = new StringBuilder();
        try {
            URLConnection connection = new URL("https://jsonip.com/").openConnection();
            connection.connect();
            try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    responseResult.append(line);
                }
            }
        } catch (IOException e) {
            System.out.printf("获取本机地址失败：%s%n", e.getMessage());
            return;
        }
        String currentHostIP = new Gson().fromJson(responseResult.toString(), JsonObject.class).get("ip").getAsString();

        // 查询主机记录
        IAcsClient client = new DefaultAcsClient(DefaultProfile.getProfile("", aliyunAccessKey, aliyunSecretKey));
        DescribeDomainRecordsRequest describeDomainRecordsRequest = new DescribeDomainRecordsRequest();
        describeDomainRecordsRequest.setDomainName(domain);
        describeDomainRecordsRequest.setRRKeyWord(subdomain);
        describeDomainRecordsRequest.setType("A");
        DescribeDomainRecordsResponse describeDomainRecordsResponse;
        try {
            describeDomainRecordsResponse = client.getAcsResponse(describeDomainRecordsRequest);
        } catch (ClientException e) {
            System.out.printf("查询域名”%s“解析记录失败：%s%n", originDomain, e.getMessage());
            return;
        }

        if (!CollectionUtils.isEmpty(describeDomainRecordsResponse.getDomainRecords())) {
            // 最新的一条解析记录
            DescribeDomainRecordsResponse.Record record = describeDomainRecordsResponse.getDomainRecords().get(0);
            String lastHostIp = record.getValue();
            if (!currentHostIP.equals(lastHostIp)) {
                // 修改解析记录
                UpdateDomainRecordRequest updateDomainRecordRequest = new UpdateDomainRecordRequest();
                // 记录ID
                updateDomainRecordRequest.setRecordId(record.getRecordId());
                // 主机记录
                updateDomainRecordRequest.setRR(subdomain);
                // 解析记录类型
                updateDomainRecordRequest.setType("A");
                // 将主机记录值改为当前主机IP
                updateDomainRecordRequest.setValue(currentHostIP);
                try {
                    // 更新主机记录
                    client.getAcsResponse(updateDomainRecordRequest);
                    System.out.printf("”%s“解析记录更新成功：”%s“ ------> ”%s“%n", originDomain, lastHostIp, currentHostIP);
                } catch (ClientException e) {
                    System.out.printf("更新域名”%s“解析记录失败：%s%n", originDomain, e.getMessage());
                }
            }
        } else {
            // 新增解析记录
            AddDomainRecordRequest addDomainRecordRequest = new AddDomainRecordRequest();
            // 主域名
            addDomainRecordRequest.setDomainName(domain);
            // 主机记录
            addDomainRecordRequest.setRR(subdomain);
            // 解析记录类型
            addDomainRecordRequest.setType("A");
            // 将主机记录值设置为当前主机IP
            addDomainRecordRequest.setValue(currentHostIP);
            try {
                // 新增主机记录
                client.getAcsResponse(addDomainRecordRequest);
                System.out.printf("”%s“解析记录新增成功：”%s“%n", originDomain, currentHostIP);
            } catch (ClientException e) {
                System.out.printf("新增域名”%s“解析记录失败：%s%n", originDomain, e.getMessage());
            }
        }
    }
}
