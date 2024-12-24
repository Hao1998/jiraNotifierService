package com.atlassian.tutorial.myPlugin.services;


import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClientBuilder;
import com.amazonaws.services.sns.model.PublishRequest;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;



@Service
public class sqsMessageProcessor {
    private final AmazonSNS snsClient = AmazonSNSClientBuilder.defaultClient();
    private static final String SNS_TOPIC_ARN = "your-sns-topic-arn";

    @Value("${aws.sqs.queue.url}")
    private String queueUrl;

    @Autowired
    private AmazonSQS sqsClient;

    @Scheduled(fixedDelay = 5000) // Poll every 5 seconds
    public void processMessages() {
        ReceiveMessageRequest receiveRequest = new ReceiveMessageRequest()
                .withQueueUrl(queueUrl)
                .withMaxNumberOfMessages(10);

        List<Message> messages = sqsClient.receiveMessage(receiveRequest).getMessages();

        for (Message message : messages) {
            // Process message and send to SNS
            Map<String, String> messageMap = new Gson().fromJson(
                    message.getBody(),
                    new TypeToken<Map<String, String>>(){}.getType()
            );

            String notificationMessage = String.format(
                    "Critical Issue: [%s] %s - Priority: %s",
                    messageMap.get("issueKey"),
                    messageMap.get("summary"),
                    messageMap.get("priority")
            );

//            PublishRequest publishRequest = new PublishRequest()
//                    .withTopicArn(SNS_TOPIC_ARN)
//                    .withMessage(notificationMessage);
//
//            snsClient.publish(publishRequest);

            // Delete processed message
            sqsClient.deleteMessage(queueUrl, message.getReceiptHandle());
        }
    }
}
