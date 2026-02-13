package com.evomind.api.integration;

import com.evomind.api.model.PayCallbackRequest;
import org.springframework.stereotype.Component;

@Component
public class PaymentSdkClient {
    public boolean verifyWechat(PayCallbackRequest req) {
        return req.tradeNo() != null && !req.tradeNo().isBlank();
    }

    public boolean verifyAlipay(PayCallbackRequest req) {
        return req.tradeNo() != null && !req.tradeNo().isBlank();
    }
}
