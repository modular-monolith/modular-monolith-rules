package io.modulith.rules.testfixtures.sender.internal;

import io.modulith.rules.testfixtures.receiver.api.ReceiverService;
import io.modulith.rules.testfixtures.sender.api.SenderService;

/**
 * Internal implementation of SenderService. Makes a direct synchronous call to
 * ReceiverService.process(), which violates an ASYNCHRONOUS communication contract
 * and constitutes a dependency that violates a NONE contract.
 */
public class SenderServiceImpl implements SenderService {

    private final ReceiverService receiverService;

    public SenderServiceImpl(ReceiverService receiverService) {
        this.receiverService = receiverService;
    }

    /**
     * Sends by calling the receiver directly. This is a synchronous call that is
     * forbidden when the contract requires asynchronous communication.
     */
    public void send() {
        receiverService.process();
    }
}
