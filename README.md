# 相关类索引

- chatroom
  - lib-clink
    - core
      - AsyncReceiveDispatcher
        - ReceiveDispatcher
          - Closeable
        - IoArgs.IoArgsEventProcessor
      - AsyncSendDispatcher
        - SendDispatcher
          - Closeable
        - IoArgs.IoArgsEventProcessor
      - Connection
        - Closeable
        - SocketChannelAdapter.OnChannelStatusChangedListener
      - IoSelectorProvider
        - IoProvider
          - Closeable
      - SendPacket
        - Packet
      - ReceivePacket
        - Packet
      - Sender
        - Closeable
      - Receiver
        - Closeable
    - impl
      - SocketChannelAdapter
        - Sender
          - Closeable
        - Receiver
          - Closeable
        - Cloneable
      - IoSelectorProvider
        - IoProvider
    - box
      - StringSendPacket
        - SendPacket
          - Packet
      - StringReceivePacket
        - ReceivePacket
          - Packet
      - FileSendPacket
        - SendPacket
          - Packet
  - sample-client
    - TcpClient
      - Connection
        - Closeable
          - SocketChannelAdapter.OnChannelStatusChangedListener
  - sample-server
    - ClientHandler
      - Connection
        - Closeable
          - SocketChannelAdapter.OnChannelStatusChangedListener
    - TcpServer
      - ClientHandler.ClientHandlerCallback