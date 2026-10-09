import socket, struct, sys, time
def pkt(i,t,b): d=struct.pack('<ii',i,t)+b.encode()+b'\x00\x00'; return struct.pack('<i',len(d))+d
def recv(s):
    n=struct.unpack('<i',s.recv(4))[0]; d=b''
    while len(d)<n: d+=s.recv(n-len(d))
    return d[8:-2].decode(errors='replace')
props=dict(l.strip().split('=',1) for l in open('run/server/server.properties') if '=' in l and not l.startswith('#'))
s=socket.create_connection(('127.0.0.1',int(props.get('rcon.port','25598')))); s.sendall(pkt(1,3,props['rcon.password'])); recv(s)
for line in sys.stdin:
    line=line.strip()
    if not line or line.startswith('# '): continue
    if line.startswith('#sleep'): time.sleep(float(line.split()[1])); continue
    if line.startswith('#signal'):
        import os; os.makedirs('run/signal', exist_ok=True); open('run/signal/' + line.split()[1], 'w').close(); continue
    s.sendall(pkt(2,2,line)); print('>',line,'=>',recv(s)[:300], flush=True)
