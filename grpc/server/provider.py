import logging
import random as rnd
import threading
import time
from enum import Enum
from concurrent import futures
from queue import Queue
from gen import mpk_pb2

stops_info = [
    mpk_pb2.StopInfo(id=1, name="Centrum Kongresowe ICE"),
    mpk_pb2.StopInfo(id=2, name="Jubilat"),
    mpk_pb2.StopInfo(id=3, name="Muzeum Narodowe"),
    mpk_pb2.StopInfo(id=4, name="Komorowskiego"),
    mpk_pb2.StopInfo(id=5, name="Filharmonia"),
]

lines_info = [
    mpk_pb2.LineInfo(
        number=144,
        direction_0='Pradnik Bialy',
        direction_1='Rzaka',
        stops=[stops_info[0], stops_info[1], stops_info[2]],
        type=mpk_pb2.LineInfo.VehicleType.A
    ),
    mpk_pb2.LineInfo(
        number=194,
        direction_0='Krowodrza Gorka',
        direction_1='Czerwone Maki P+R',
        stops=[stops_info[0], stops_info[1], stops_info[2]],
        type=mpk_pb2.LineInfo.VehicleType.A
    ),
    mpk_pb2.LineInfo(
        number=1,
        direction_0='Wzgorza Krzeslawickie',
        direction_1='Salwator',
        stops=[stops_info[3], stops_info[1], stops_info[4]],
        type=mpk_pb2.LineInfo.VehicleType.T
    )
]

stop2lines = {
    1: [144, 194],
    2: [144, 194, 1],
    3: [144, 194],
    4: [1],
    5: [1]
}


# Server - provider communication
class ProviderResponse:
    def __init__(self, type, content):
        self.type = type
        self.content = content


class MsgType(Enum):
    OK = 0
    FINISHED = 1
    BAD_REQUEST = 2


class Task:
    def __init__(self, event, future, result_queue):
        self.event = event
        self.future = future
        self.queue = result_queue

    def get(self):
        return self.queue.get()

    def cancel(self):
        try:
            self.event.set()
            self.future.cancel()
            futures.wait([self.future], timeout=5)
        except futures.TimeoutError:
            logging.info('Timed out waiting for provider task to close.')


class MpkProvider:
    def __init__(self, executor):
        self.executor = executor

    @staticmethod
    def get_lines():
        return lines_info

    def observe(self, request):
        result_queue = Queue(maxsize=10)
        event = threading.Event()
        future = self.executor.submit(self._observe, request, event, result_queue)
        return Task(event, future, result_queue)

    @staticmethod
    def _observe(request, event, queue):
        available_lines = stop2lines.get(request.stop_id, None)

        # Handle erroneous requests.
        if not available_lines:
            msg = ProviderResponse(type=MsgType.BAD_REQUEST, content='Invalid stop ID.')
            queue.put_nowait(msg)
            return

        common_lines = [line for line in request.lines if line.number in available_lines]
        if not common_lines:
            msg = ProviderResponse(type=MsgType.BAD_REQUEST, content='Invalid lines for given stop.')
            queue.put_nowait(msg)
            return

        # Get observations for correct requests.
        # Note: duration would normally be a time interval
        for _ in range(request.duration):
            # Exit early if communication breaks down.
            if event.is_set():
                break

            n_observed = rnd.randint(1, len(common_lines))
            observed = rnd.sample(common_lines, k=n_observed)
            msg = ProviderResponse(type=MsgType.OK, content=observed)
            try:
                queue.put_nowait(msg)
            except queue.Full:
                logging.info('Server too slow, skipping...')

            logging.info(f'Observed vehicles of: {observed}')
            time.sleep(rnd.randint(1, 2))

        msg = ProviderResponse(type=MsgType.FINISHED, content=None)
        queue.put(msg)

        logging.info('Provider done.')
