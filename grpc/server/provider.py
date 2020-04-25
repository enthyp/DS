import logging
import random as rnd
import time
from enum import Enum
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
class ProviderMsg:
    def __init__(self, type, content):
        self.type = type
        self.content = content


class MsgType(Enum):
    OK = 0
    FINISHED = 1
    BAD_REQUEST = 2


class MpkProvider:
    def __init__(self, executor):
        self.executor = executor

    @staticmethod
    def get_lines():
        return lines_info

    def observe(self, stop, lines):
        result_queue = Queue(maxsize=10)
        self.executor.submit(self._observe, stop, lines, result_queue)
        return result_queue

    @staticmethod
    def _observe(stop, lines, queue):
        n_observations = rnd.randint(1, 10)  # normally would be time limited or sth
        available_lines = stop2lines.get(stop, None)

        # Handle erroneous requests.
        if not available_lines:
            msg = ProviderMsg(type=MsgType.BAD_REQUEST, content='Invalid stop ID.')
            queue.put(msg)
            return

        common_lines = [line for line in lines if line.number in available_lines]
        if not common_lines:
            msg = ProviderMsg(type=MsgType.BAD_REQUEST, content='Invalid lines for given stop.')
            queue.put(msg)
            return

        # Get observations for correct requests.
        for _ in range(n_observations):
            n_observed = rnd.randint(1, len(common_lines))
            observed = rnd.sample(common_lines, k=n_observed)
            msg = ProviderMsg(type=MsgType.OK, content=observed)
            queue.put(msg)

            logging.info(f'Observed vehicles of: {observed}')
            time.sleep(rnd.randint(1, 2))
        msg = ProviderMsg(type=MsgType.FINISHED, content=None)
        queue.put(msg)

        logging.info('Provider done.')
