import concurrent.futures as cf
import logging
import socket
import threading
from queue import Queue
from threading import Lock
from protocol import Connection, MAX_UDP_SIZE, recv_udp, send_udp

logging.basicConfig(level='DEBUG')

MAX_CLIENT_QUEUE = 10


class ClientHandler:

    LOGIN_MSG = 'Choose a nickname.'
    HELLO_MSG = 'You are in, {}!'
    NICK_TAKEN_MSG = 'Nickname {} is taken, choose another one.'

    SHUTDOWN_MSG = 'Server shut down and you were disconnected.'

    def __init__(self, sock, addr, server):
        self.server = server
        self.conn = Connection(sock, addr)
        self.inbox = Queue(maxsize=MAX_CLIENT_QUEUE)
        self.nickname = None
        self.channel = None

    @property
    def addr(self):
        return self.conn.addr

    def init(self):
        self.conn.send_msg(self.LOGIN_MSG)

        while True:
            nickname = self.conn.recv_msg()

            try:
                self.server.register_client(nickname, self)
                self.server.addr_client[self.addr] = self

                self.nickname = nickname
                self.conn.send_msg(self.HELLO_MSG.format(nickname))
                break
            except NickTakenError:
                self.conn.send_msg(self.NICK_TAKEN_MSG.format(nickname))

        # Connection fully established.
        self.server.subscribe(self.nickname, 'default')
        self.channel = self.server.channels['default']
        self.in_channel()

    def shutdown(self):
        try:
            self.conn.send_msg(self.SHUTDOWN_MSG)
        except ConnectionError:
            pass
        self.conn.close()

    def in_channel(self):
        receiver = threading.Thread(target=self.process_inbox, daemon=True)
        receiver.start()

        while True:
            msg = self.conn.recv_msg()
            msg = '{}: {}'.format(self.nickname, msg)
            self.channel.publish(('tcp', msg), self.nickname)

    def process_inbox(self):
        while True:
            kin, msg = self.inbox.get()

            if kin == 'udp':
                send_udp(msg, self.addr)
            else:
                self.conn.send_msg(msg)

    def push(self, msg):
        self.inbox.put(msg)

    def handle_udp(self, msg):
        self.channel.publish(('udp', msg), self.nickname)


class Channel:
    def __init__(self, name):
        self.name = name
        self.clients = {}
        self.lock = Lock()

    def sub(self, name, client):
        self.clients[name] = client

    def unsub(self, name):
        del self.clients[name]

    def publish(self, msg, sender_name):
        with self.lock:
            for name, c in self.clients.items():
                if name != sender_name:
                    c.push(msg)


class NickTakenError(Exception):
    pass


class SocketServer:
    def __init__(self, host='127.0.0.1', port=8000):
        self.host = host
        self.port = port
        self.clients = {}
        self.channels = {}
        self.addr_client = {}

        self.pool = None
        self.cl_lock = threading.Lock()
        self.ch_lock = threading.Lock()

    def run(self):
        with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
            s.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
            s.bind((self.host, self.port))
            s.listen()

            udp_receiver = threading.Thread(target=self.udp_run, daemon=True)
            udp_receiver.start()

            self.channels['default'] = Channel('default')

            try:
                self.pool = cf.ThreadPoolExecutor(max_workers=10)
                logging.info('Server running...')

                while True:
                    conn, addr = s.accept()
                    client = ClientHandler(conn, addr, self)

                    self.pool.submit(fn=client.init)
                    logging.info('Client connection: {} {}'.format(*addr))
            except KeyboardInterrupt:
                for c in self.clients.values():
                    c.shutdown()
                self.pool.shutdown()

    def udp_run(self):
        with socket.socket(socket.AF_INET, socket.SOCK_DGRAM) as s:
            s.bind((self.host, self.port))

            while True:
                msg, addr = s.recvfrom(MAX_UDP_SIZE)
                sender = self.addr_client.get(addr, None)

                if sender:
                    sender.handle_udp(msg.decode())

    def register_client(self, name, client):
        with self.cl_lock:
            if name in self.clients:
                raise NickTakenError
            self.clients[name] = client

    def remove_client(self, name):
        with self.cl_lock:
            del self.clients[name]

    def subscribe(self, name, channel):
        with self.ch_lock:
            client = self.clients[name]
            self.channels[channel].sub(name, client)

    def unsubscribe(self, name, channel):
        with self.ch_lock:
            self.channels[channel].unsub(name)


if __name__ == '__main__':
    server = SocketServer()
    server.run()
