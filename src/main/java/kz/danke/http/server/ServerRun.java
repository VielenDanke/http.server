package kz.danke.http.server;

import kz.danke.http.server.factory.FactoryPacker;
import kz.danke.http.server.factory.HttpAnnotationHandlerFactory;
import kz.danke.http.server.factory.impl.AnnotationFactoryPacker;
import kz.danke.http.server.factory.impl.HttpAnnotationHandlerFactoryImpl;

public class ServerRun {

    public static void start(String packageToScan, String host, Integer port) throws Exception {
        FactoryPacker packer = new AnnotationFactoryPacker(packageToScan);

        HttpAnnotationHandlerFactory factory = new HttpAnnotationHandlerFactoryImpl();

        packer.packaging(factory);

        HostPortConfig hostPortConfig = ServerAssistance.setPortHostIfEmpty(host, port);

        Server server = new Server(factory, hostPortConfig);

        server.bootstrap();
    }

    public static void start(String packageToScan) throws Exception {
        start(packageToScan, null, null);
    }

    public static void main(String[] args) throws Exception {
        start("kz.danke.http.server");
    }
}
