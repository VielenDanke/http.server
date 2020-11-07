package kz.danke.http.server;

import kz.danke.http.server.factory.FactoryPacker;
import kz.danke.http.server.factory.HttpAnnotationHandlerFactory;
import kz.danke.http.server.factory.impl.AnnotationFactoryPacker;
import kz.danke.http.server.factory.impl.HttpAnnotationHandlerFactoryImpl;

public class ServerRun {

    public static void start(String packageToScan, String host, int port) throws Exception {
        FactoryPacker packer = new AnnotationFactoryPacker(packageToScan);

        HttpAnnotationHandlerFactory factory = new HttpAnnotationHandlerFactoryImpl();

        packer.packaging(factory);

        Server server = new Server(factory, host, port);

        server.bootstrap();
    }

    public static void main(String[] args) throws Exception {
        start("kz.danke.http.server", "localhost", 8444);
    }
}
