import argparse
import socket
import threading
from protocol import Connection, recv_udp, send_udp


class Client:
    def __init__(self, server_host='127.0.0.1', server_port=8000, client_host='127.0.0.1', client_port=8001):
        self.server_addr = (server_host, server_port)
        self.client_addr = (client_host, client_port)
        self.conn = None

    def run(self):
        with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
            s.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
            s.bind(self.client_addr)
            s.connect(self.server_addr)
            self.conn = Connection(s, self.server_addr)

            receiver = threading.Thread(target=self.receive, daemon=True)
            receiver.start()
            udp_receiver = threading.Thread(target=self.receive_udp, daemon=True)
            udp_receiver.start()

            try:
                while True:
                    msg = input()
                    if msg == 'U':
                        with open('ascii.txt', 'r') as ascii_art:
                            msg = ascii_art.read()
                            send_udp(msg, self.server_addr, self.client_addr)
                    elif msg == 'M':
                        pass  # TODO!
                    else:
                        self.conn.send_msg(msg)
            except KeyboardInterrupt:
                print('Received Ctrl+C.')
            except ConnectionError:
                print('Connection to server closed.')

    def receive(self):
        try:
            while True:
                msg = self.conn.recv_msg()
                print('>>> {}'.format(msg))
        except ConnectionError:
            self.conn.close()

    def receive_udp(self):
        try:
            while True:
                msg = recv_udp(self.client_addr)
                print(msg)
        except ConnectionError:
            self.conn.close()


def parse_args():
    parser = argparse.ArgumentParser()
    parser.add_argument('port', type=int, nargs='?', default=8001)
    args = parser.parse_args()
    return args.port


if __name__ == '__main__':
    port = parse_args()
    client = Client(client_port=port)
    client.run()
