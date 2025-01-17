/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.seata.discovery.loadbalance;

import io.seata.discovery.registry.RegistryFactory;
import io.seata.discovery.registry.RegistryService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static io.seata.common.DefaultValues.DEFAULT_TX_GROUP;

/**
 * The type Load balance factory test.
 *
 */
public class LoadBalanceFactoryTest {

    private static final String XID = "XID";

    /**
     * Test get registry.
     *
     * @param loadBalance the load balance
     * @throws Exception the exception
     */
    @ParameterizedTest
    @MethodSource("instanceProvider")
    @Disabled
    public void testGetRegistry(LoadBalance loadBalance) throws Exception {
        Assertions.assertNotNull(loadBalance);
        RegistryService registryService = RegistryFactory.getInstance();
        InetSocketAddress address1 = new InetSocketAddress("127.0.0.1", 8091);
        InetSocketAddress address2 = new InetSocketAddress("127.0.0.1", 8092);
        registryService.register(address1);
        registryService.register(address2);
        List<InetSocketAddress> addressList = registryService.lookup(DEFAULT_TX_GROUP);
        InetSocketAddress balanceAddress = loadBalance.select(addressList, XID);
        Assertions.assertNotNull(balanceAddress);
    }

    /**
     * Test get address.
     *
     * @param loadBalance the load balance
     * @throws Exception the exception
     */
    @ParameterizedTest
    @MethodSource("instanceProvider")
    @Disabled
    public void testUnRegistry(LoadBalance loadBalance) throws Exception {
        RegistryService registryService = RegistryFactory.getInstance();
        InetSocketAddress address = new InetSocketAddress("127.0.0.1", 8091);
        registryService.unregister(address);
    }

    /**
     * Test subscribe.
     *
     * @param loadBalance the load balance
     * @throws Exception the exception
     */
    @ParameterizedTest
    @MethodSource("instanceProvider")
    @Disabled
    public void testSubscribe(LoadBalance loadBalance) throws Exception {
        Assertions.assertNotNull(loadBalance);
        RegistryService registryService = RegistryFactory.getInstance();
        InetSocketAddress address1 = new InetSocketAddress("127.0.0.1", 8091);
        InetSocketAddress address2 = new InetSocketAddress("127.0.0.1", 8092);
        registryService.register(address1);
        registryService.register(address2);
        List<InetSocketAddress> addressList = registryService.lookup(DEFAULT_TX_GROUP);
        InetSocketAddress balanceAddress = loadBalance.select(addressList, XID);
        Assertions.assertNotNull(balanceAddress);
        //wait trigger testUnRegistry
        TimeUnit.SECONDS.sleep(30);
        List<InetSocketAddress> addressList1 = registryService.lookup(DEFAULT_TX_GROUP);
        Assertions.assertEquals(1, addressList1.size());
    }

    /**
     * Test get address.
     *
     * @param loadBalance the load balance
     * @throws Exception the exception
     */
    @ParameterizedTest
    @MethodSource("instanceProvider")
    public void testGetAddress(LoadBalance loadBalance) throws Exception {
        Assertions.assertNotNull(loadBalance);
        InetSocketAddress address = new InetSocketAddress("127.0.0.1", 8091);
        List<InetSocketAddress> addressList = new ArrayList<>();
        addressList.add(address);
        InetSocketAddress balanceAddress = loadBalance.select(addressList, XID);
        Assertions.assertEquals(address, balanceAddress);
    }

    /**
     * Instance provider object [ ] [ ].
     *
     * @return the object [ ] [ ]
     */
    static Stream<Arguments> instanceProvider() {
        LoadBalance loadBalance = LoadBalanceFactory.getInstance();
        return Stream.of(
                Arguments.of(loadBalance)
        );
    }
}
