package top.mqk233.ddns.schedule;

import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.alidns.model.v20150109.AddDomainRecordRequest;
import com.aliyuncs.alidns.model.v20150109.DescribeDomainRecordsRequest;
import com.aliyuncs.alidns.model.v20150109.DescribeDomainRecordsResponse;
import com.aliyuncs.alidns.model.v20150109.UpdateDomainRecordRequest;
import com.aliyuncs.profile.DefaultProfile;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.util.CollectionUtils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;

/**
 * 动态域名解析定时器
 *
 * @since 2021-02-09
 */
@Configuration
public class DynamicDNSSchedule {
    @Value("${aliyun.access-key}")
    private String aliAccessKey;

    @Value("${aliyun.secret-key}")
    private String aliSecretKey;

    @Value("${aliyun.domain}")
    private String domain;

    @Scheduled(fixedDelayString = "${aliyun.execute-interval}")
    public void aliDynamicDNS() throws Exception {
        IAcsClient client = new DefaultAcsClient(DefaultProfile.getProfile("", aliAccessKey, aliSecretKey));
        // 查询指定二级域名的最新解析记录
        DescribeDomainRecordsRequest describeDomainRecordsRequest = new DescribeDomainRecordsRequest();
        // 主域名
        describeDomainRecordsRequest.setDomainName(domain);
        // 主机记录
        describeDomainRecordsRequest.setRRKeyWord("@");
        // 解析记录类型
        describeDomainRecordsRequest.setType("A");
        DescribeDomainRecordsResponse describeDomainRecordsResponse = client.getAcsResponse(describeDomainRecordsRequest);
        List<DescribeDomainRecordsResponse.Record> domainRecords = describeDomainRecordsResponse.getDomainRecords();
        // 使用jsonip获取当前主机公网IP
        URLConnection urlConnection = new URL("https://jsonip.com/").openConnection();
        urlConnection.connect();
        StringBuilder responseResult = new StringBuilder();
        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()))) {
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                responseResult.append(line);
            }
        }
        if (responseResult.length() == 0) {
            System.out.println("获取本机公网IP失败");
            return;
        }
        // 从response中获取当前主机公网IP
        String currentHostIP = new Gson().fromJson(responseResult.toString(), JsonObject.class).get("ip").getAsString();
        if (!CollectionUtils.isEmpty(domainRecords)) {
            // 最新的一条解析记录
            DescribeDomainRecordsResponse.Record record = domainRecords.get(0);
            String lastHostIp = record.getValue();
            if (!currentHostIP.equals(lastHostIp)) {
                // 修改解析记录
                UpdateDomainRecordRequest updateDomainRecordRequest = new UpdateDomainRecordRequest();
                // 记录ID
                updateDomainRecordRequest.setRecordId(record.getRecordId());
                // 主机记录
                updateDomainRecordRequest.setRR("@");
                // 解析记录类型
                updateDomainRecordRequest.setType("A");
                // 将主机记录值改为当前主机IP
                updateDomainRecordRequest.setValue(currentHostIP);
                client.getAcsResponse(updateDomainRecordRequest);
                System.out.println("\"" + domain + "\"解析记录更新成功：" + lastHostIp + " ------> " + currentHostIP);
            }
        } else {
            // 新增解析记录
            AddDomainRecordRequest addDomainRecordRequest = new AddDomainRecordRequest();
            // 主域名
            addDomainRecordRequest.setDomainName(domain);
            // 主机记录
            addDomainRecordRequest.setRR("@");
            // 解析记录类型
            addDomainRecordRequest.setType("A");
            // 将主机记录值设置为当前主机IP
            addDomainRecordRequest.setValue(currentHostIP);
            client.getAcsResponse(addDomainRecordRequest);
            System.out.println("\"" + domain + "\"解析记录新增成功：" + currentHostIP);
        }
    }
}
