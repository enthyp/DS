import grpc
import logging
from concurrent import futures
from gen import mpk_pb2, mpk_pb2_grpc
from provider import MpkProvider, MsgType

logging.basicConfig(level=logging.INFO)


class MpkPublisher(mpk_pb2_grpc.MpkPublisherServicer):
    def __init__(self, mpk_provider):
        self.provider = mpk_provider

    def GetSchedule(self, request, context):
        response = mpk_pb2.Schedule()
        lines = self.provider.get_lines()
        response.lines.extend(lines)

        return response

    def Subscribe(self, request, context):
        result_queue = self.provider.observe(request.stop_id, request.lines)

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
                return mpk_pb2.NotifyResponse()


def gtfs():
    from google.transit import gtfs_realtime_pb2
    from pprint import pprint

    feed = gtfs_realtime_pb2.FeedMessage()
    with open('VehiclePositions_A.pb', 'rb') as v_file:
        content = v_file.read()
    feed.ParseFromString(content)
    pprint(feed.entity)


def serve():
    executor = futures.ThreadPoolExecutor(max_workers=10)
    server = grpc.server(executor)
    mpk_provider = MpkProvider(executor)
    mpk_publisher = MpkPublisher(mpk_provider)

    mpk_pb2_grpc.add_MpkPublisherServicer_to_server(mpk_publisher, server)
    server.add_insecure_port('[::]:50051')
    server.start()

    logging.info('Server is running...')
    server.wait_for_termination()


if __name__ == '__main__':
    serve()
