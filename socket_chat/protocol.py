import socket

LEN_BYTES = 2
BYTE_ORDER = 'big'
MAX_UDP_SIZE = 8096


class MessageSizeError(ConnectionError):
    pass


class Connection:
    def __init__(self, sock, addr):
        self.addr = addr
        self.sock = sock

    def send_msg(self, msg):
        try:
            msg_len = len(msg).to_bytes(LEN_BYTES, BYTE_ORDER)
            if self.sock.sendall(msg_len + msg.encode()) == 0:
                raise ConnectionError
        except OverflowError:
            raise MessageSizeError
        except BrokenPipeError:
            raise ConnectionError

    def recv_msg(self):
        try:
            # Receive message length in bytes.
            received = b''
            while len(received) < LEN_BYTES:
                recv = self.sock.recv(LEN_BYTES - len(received))
                if not recv:
                    raise ConnectionError
                received += recv

            msg_len = int.from_bytes(received, BYTE_ORDER)

            # Receive actual bytes of the message.
            received = b''
            while len(received) < msg_len:
                recv = self.sock.recv(msg_len - len(received))
                if not recv:
                    raise ConnectionError
                received += recv

            return received.decode()
        except BrokenPipeError:
            raise ConnectionError

    def close(self):
        self.sock.shutdown(socket.SHUT_RDWR)
        self.sock.close()


def send_udp(msg, dest_addr, source_addr=None):
    with socket.socket(socket.AF_INET, socket.SOCK_DGRAM) as u_sock:
        u_sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)

        if source_addr:
            u_sock.bind(source_addr)

        u_sock.sendto(msg.encode(), dest_addr)
