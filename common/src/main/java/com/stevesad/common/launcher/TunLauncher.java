package com.stevesad.common.launcher;

import com.stevesad.common.consumer.TunPacketConsumer;
import com.stevesad.common.publisher.TunPacketPublisher;
import com.stevesad.common.tun.TunDevice;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TunLauncher extends TransactionalLauncher {

    private final TunDevice tunDevice;
    private final TunPacketPublisher tunPacketPublisher;
    private final TunPacketConsumer tunPacketConsumer;

    public void start(String address, int maskLength, int mtu) throws Exception {
        super.addAction(() -> tunDevice.start(address, maskLength, mtu), tunDevice::close);
        super.addAction(tunPacketPublisher::startPollingLoop, tunPacketPublisher::stopPollingLoop);
        super.addAction(tunPacketConsumer::startPollingLoop, tunPacketConsumer::stopPollingLoop);
        super.executeSequence();
    }

    @PreDestroy
    public void stop() {
        super.rollbackSequence();
    }
}
