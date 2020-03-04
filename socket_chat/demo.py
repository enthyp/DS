from multiprocessing import Process
from client import Client
from server import SocketServer


def client_process(client, nick):
    print('xD')


if __name__ == '__main__':
    server = SocketServer()
    server.run()

    client1, client2 = Client(), Client()
    p1 = Process(target=lambda: client_process(client1, 'C1'))
    p2 = Process(target=lambda: client_process(client2, 'C2'))
    p1.start()
    p2.start()

    p1.join()
    p2.join()
