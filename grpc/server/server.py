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
        result_queue = self.provider.observe(request.stop_id, request.duration, request.lines)

        while True:
            result = result_queue.get()

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
        logging.info('Closing stream.')

        # IMPORTANT:
        # https://stackoverflow.com/questions/43948975/grpc-unary-streaming-continues-after-disconnect-blocks-on-interrupt
        # Serious bug in Python grpcio - server doesn't notice client forceful disconnect - threads leak...


def serve():
    executor = futures.ThreadPoolExecutor(max_workers=10)
    server = grpc.server(
        executor,
        options=(
            ('grpc.keepalive_time_ms', 5000),
            # send keepalive ping every 5 sec, default is 2 hours
            ('grpc.keepalive_timeout_ms', 1000),
            # # keepalive ping time out after 1 sec, default is 20 sec
        )
    )
    mpk_provider = MpkProvider(executor)
    mpk_publisher = MpkPublisher(mpk_provider)

    mpk_pb2_grpc.add_MpkPublisherServicer_to_server(mpk_publisher, server)
    server.add_insecure_port('localhost:50051')
    server.start()

    logging.info('Server is running...')
    server.wait_for_termination()


if __name__ == '__main__':
    serve()
