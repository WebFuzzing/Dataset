# TCP -> unix socket bridge, so the management API can reach the agent socket from another container/host.
# Usage: python3 mgmtapi-bridge.py <agent unix socket> <tcp port>
import socket
import sys
import threading

AGENT_SOCKET = sys.argv[1]
PORT = int(sys.argv[2])


def pipe(src, dst):
    try:
        while True:
            data = src.recv(65536)
            if not data:
                break
            dst.sendall(data)
    except OSError:
        pass
    finally:
        for s in (src, dst):
            try:
                s.shutdown(socket.SHUT_RDWR)
            except OSError:
                pass
        src.close()


server = socket.create_server(("0.0.0.0", PORT))
while True:
    client, _ = server.accept()
    agent = socket.socket(socket.AF_UNIX, socket.SOCK_STREAM)
    try:
        agent.connect(AGENT_SOCKET)
    except OSError:
        # the agent opens the socket only once Cassandra is up
        agent.close()
        client.close()
        continue
    threading.Thread(target=pipe, args=(client, agent), daemon=True).start()
    threading.Thread(target=pipe, args=(agent, client), daemon=True).start()
