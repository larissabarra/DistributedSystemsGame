import socket
import thread
import json
import time
import sys

HOST = '127.0.0.1'
PORT = 8001
BUFFER_SIZE = 4096
ENCODE_TYPE = 'utf-8'

nodes  = []

def process_enternode(cliente_id, ip, con):
    try:
        response =  {'node_id':cliente_id, 'cluster': nodes}
        response = json.dumps(response)
        nodes.append(tuple([cliente_id, ip]))
        con.send(response)
        print 'Novo node adicionado: ', response
    except:
        raise Exception('Falha ao processar entrada de novo node: ', sys.exc_info()[1])

def process_exitnode(cliente_id):
    try:
        find_node = [item for item in nodes if item[0] == cliente_id]
        nodes.remove(find_node[0])
        print 'Node removido: ', cliente_id
    except:
        raise Exception('Falha ao processar remocao de node: ', sys.exc_info()[1])

def conectado(con, cliente):
  print 'Recebendo conexao de: ', cliente

  while True:
      try:
          timestamp = int(time.time())
          ip = cliente[0]
          cliente_id = str(timestamp) + ':' + ip + ':' + str(cliente[1])

          msg = con.recv(BUFFER_SIZE)
          msg = msg.decode(ENCODE_TYPE)
          msg = json.loads(msg)

          if not msg: break

          print "Identificador atribuido: " + cliente_id

          if (msg['type'] == 'enter'):
              process_enternode(cliente_id, ip, con)
          elif(msg['type'] == 'exit'):
              process_exitnode(msg['id'])

      except:
          print 'Erro ao tentar processar cliente: ', sys.exc_info()[1]
          break

  print 'Finalizando conexao do cliente', cliente
  con.close()
  thread.exit()

tcp = socket.socket(socket.AF_INET, socket.SOCK_STREAM)

orig = (HOST, PORT)

tcp.bind(orig)
tcp.listen(1)

while True:
  con, cliente = tcp.accept()
  thread.start_new_thread(conectado, tuple([con, cliente]))

tcp.close()
