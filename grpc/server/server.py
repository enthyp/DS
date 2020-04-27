import grpc
import logging
from concurrent import futures
from gen import mpk_pb2, mpk_pb2_grpc
from provider import MpkProvider, MsgType

logging.basicConfig(level=logging.DEBUG)


class MpkPublisher(mpk_pb2_grpc.MpkPublisherServicer):
    def __init__(self, mpk_provider):
        self.provider = mpk_provider

    def GetSchedule(self, request, context):
        logging.info(f'GetSchedule received.')
        response = mpk_pb2.Schedule()
        lines = self.provider.get_lines()
        response.lines.extend(lines)

        return response

    def Subscribe(self, request, context):
        logging.info(f'Subscribe received: {request}')
        task = self.provider.observe(request)

        try:
            while True:
                result = task.get()

                if result.type == MsgType.OK:
                    yield mpk_pb2.NotifyResponse(lines=result.content)
                elif result.type == MsgType.FINISHED:
                    break
                else:
                    msg = result.content
                    context.set_details(msg)
                    context.set_code(grpc.StatusCode.INVALID_ARGUMENT)
                    yield mpk_pb2.NotifyResponse()
                    break
                logging.info('Sent notification...')
        finally:
            logging.info('Closing stream.')
            task.cancel()


def serve():
    executor = futures.ThreadPoolExecutor(max_workers=10)
    server = grpc.server(
        executor,
        # extremely short timeouts just to show
        options=(
            ('grpc.keepalive_time_ms', 100),
            # send keepalive ping every 0.1 sec, default is 2 hours
            ('grpc.keepalive_timeout_ms', 100),
            # # keepalive ping time out after 0.1 sec, default is 20 sec
        )
    )
    mpk_provider = MpkProvider(executor)
    mpk_publisher = MpkPublisher(mpk_provider)

    mpk_pb2_grpc.add_MpkPublisherServicer_to_server(mpk_publisher, server)
    server.add_insecure_port('192.168.100.106:50051')
    server.start()

    logging.info('Server is running...')
    server.wait_for_termination()


if __name__ == '__main__':
    serve()
