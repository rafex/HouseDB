package com.rafex.housedb.bootstrap;

import com.rafex.housedb.core.ItemFinderService;
import com.rafex.housedb.infra.postgres.PostgresItemRepository;
import com.rafex.housedb.transport.jetty.JettyTransport;

public final class HouseDbApplication {

    private HouseDbApplication() {
    }

    public static void main(String[] args) {
        var repository = new PostgresItemRepository();
        var finderService = new ItemFinderService(repository);
        var transport = new JettyTransport();

        transport.start();
        finderService.findByCategory("camping");

        System.out.println("HouseDB bootstrap ready");
    }
}
