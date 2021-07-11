package top.mqk233.ddns.schedule;

import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.alidns.model.v20150109.*;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.profile.DefaultProfile;
import com.aliyuncs.utils.StringUtils;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

/**
 * 阿里云域名解析定时器
 *
 * @author mqk233
 * @since 2021-02-09
 */
@Configuration
public class AliyunDNSSchedule {
    @Value("${aliyun.access-key-id}")
    private String accessKeyId;

    @Value("${aliyun.access-key-secret}")
    private String accessKeySecret;

    @Resource
    private ApplicationArguments arguments;

    private IAcsClient client;

    @PostConstruct
    public void init() {
        client = new DefaultAcsClient(DefaultProfile.getProfile("", accessKeyId, accessKeySecret));
    }

    @Scheduled(fixedDelayString = "${aliyun.update-interval}")
    public void aliyunDNS() {
        // 校验输入参数
        if (arguments.getSourceArgs() == null || arguments.getSourceArgs().length != 1) {
            System.out.println("参数输入错误，请输入本机地址需要指向的域名");
            return;
        }
        // 用户输入的域名参数
        String originDomain = arguments.getSourceArgs()[0];

        // 获取域名信息
        GetMainDomainNameResponse mainDomainNameResponse;
        try {
            GetMainDomainNameRequest mainDomainNameRequest = new GetMainDomainNameRequest();
            mainDomainNameRequest.setInputString(originDomain);
            mainDomainNameResponse = client.getAcsResponse(mainDomainNameRequest);
        } catch (ClientException e) {
            System.out.printf("域名“%s”输入有误：%s%n", originDomain, e.getMessage());
            return;
        }

        // 获取本机地址
        String localAddress;
        try {
            URLConnection connection = new URL("https://jsonip.com/").openConnection();
            connection.connect();
            try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                StringBuilder response = new StringBuilder();
                String line;
                while (!StringUtils.isEmpty(line = bufferedReader.readLine())) {
                    response.append(line);
                }
                localAddress = new Gson().fromJson(response.toString(), JsonObject.class).get("ip").getAsString();
            }
        } catch (IOException e) {
            System.out.printf("获取本机地址失败：%s%n", e.getMessage());
            return;
        }

        // 查询域名解析记录
        DescribeDomainRecordsResponse describeDomainRecordsResponse;
        try {
            DescribeDomainRecordsRequest describeDomainRecordsRequest = new DescribeDomainRecordsRequest();
            describeDomainRecordsRequest.setDomainName(mainDomainNameResponse.getDomainName());
            describeDomainRecordsRequest.setRRKeyWord(mainDomainNameResponse.getRR());
            describeDomainRecordsRequest.setType("A");
            describeDomainRecordsResponse = client.getAcsResponse(describeDomainRecordsRequest);
        } catch (ClientException e) {
            System.out.printf("查询”%s“解析记录失败：%s%n", originDomain, e.getMessage());
            return;
        }

        // 当前域名没有解析记录或者存在多条解析记录则另外添加一条解析记录
        if (describeDomainRecordsResponse.getTotalCount() != 1) {
            try {
                AddDomainRecordRequest addDomainRecordRequest = new AddDomainRecordRequest();
                addDomainRecordRequest.setDomainName(mainDomainNameResponse.getDomainName());
                addDomainRecordRequest.setRR(mainDomainNameResponse.getRR());
                addDomainRecordRequest.setType("A");
                addDomainRecordRequest.setValue(localAddress);
                client.getAcsResponse(addDomainRecordRequest);
                System.out.printf("”%s“解析记录新增成功：”%s“%n", originDomain, localAddress);
            } catch (ClientException e) {
                System.out.printf("新增”%s“解析记录失败：%s%n", originDomain, e.getMessage());
            }
            return;
        }

        // 当前域名只有一条解析记录则直接修改
        DescribeDomainRecordsResponse.Record domainRecord = describeDomainRecordsResponse.getDomainRecords().get(0);
        if (!localAddress.equals(domainRecord.getValue())) {
            try {
                UpdateDomainRecordRequest updateDomainRecordRequest = new UpdateDomainRecordRequest();
                updateDomainRecordRequest.setRecordId(domainRecord.getRecordId());
                updateDomainRecordRequest.setRR(mainDomainNameResponse.getRR());
                updateDomainRecordRequest.setType("A");
                updateDomainRecordRequest.setValue(localAddress);
                client.getAcsResponse(updateDomainRecordRequest);
                System.out.printf("”%s“解析记录更新成功：”%s“ ------> ”%s“%n", originDomain, domainRecord.getValue(), localAddress);
            } catch (ClientException e) {
                System.out.printf("更新”%s“解析记录失败：%s%n", originDomain, e.getMessage());
            }
        }
    }
}
