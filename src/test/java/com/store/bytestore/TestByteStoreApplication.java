package com.store.bytestore;

import org.springframework.boot.SpringApplication;

public class TestByteStoreApplication {

    public static void main(String[] args) {
        SpringApplication.from(ByteStoreApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
