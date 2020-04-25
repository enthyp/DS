from grpc_tools import protoc

protoc.main((
    '',
    '-I../client/src/main/proto',
    '--python_out=./gen',
    '--grpc_python_out=./gen',
    '../client/src/main/proto/mpk.proto',
))
