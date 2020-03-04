import argparse
import select
import socket
import threading
from protocol import Connection, MAX_UDP_SIZE, send_udp

MULTICAST_ADDR = ('224.0.0.1', 8000)


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

                    if msg == 'U' or msg == 'M':
                        with open('ascii.txt', 'r') as ascii_art:
                            content = ascii_art.read()

                        if msg == 'U':
                            addr = self.server_addr
                        else:
                            addr = MULTICAST_ADDR
                        send_udp(content, addr, self.client_addr)
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
        with socket.socket(socket.AF_INET, socket.SOCK_DGRAM) as u_sock:
            with socket.socket(socket.AF_INET, socket.SOCK_DGRAM) as m_sock:
                u_sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
                u_sock.bind(self.client_addr)

                host = socket.gethostbyname(socket.gethostname())
                m_sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
                m_sock.setsockopt(socket.SOL_IP, socket.IP_ADD_MEMBERSHIP,
                                  socket.inet_aton(MULTICAST_ADDR[0]) + socket.inet_aton(host))
                m_sock.bind(MULTICAST_ADDR)

                ev_loop = select.epoll()
                u_fd, m_fd = u_sock.fileno(), m_sock.fileno()
                ev_loop.register(u_fd, select.EPOLLIN)
                ev_loop.register(m_fd, select.EPOLLIN)

                try:
                    while True:
                        events = ev_loop.poll()
                        for file_no, event in events:
                            if event | select.EPOLLIN:
                                # TODO: received in parts??
                                if file_no == u_fd:
                                    msg = u_sock.recv(MAX_UDP_SIZE)
                                else:
                                    msg, addr = m_sock.recvfrom(MAX_UDP_SIZE)
                                    if addr == self.client_addr:
                                        continue
                                print(msg.decode())
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
