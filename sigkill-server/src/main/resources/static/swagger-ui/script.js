(function () {
    "use strict";

    // import SockJs from 'sockjs-client';
    // import Stomp from 'webstomp-client';

    const serverBaseInput = document.getElementById("serverBase");
    const roomIdInput = document.getElementById("roomId");
    const messageInput = document.getElementById("message");
    const roomsViewEl = document.getElementById("roomsView");
    const logEl = document.getElementById("log");
    const loginBtn = document.getElementById("loginBtn");
    const seedRoomsBtn = document.getElementById("seedRoomsBtn");
    const loadRoomsBtn = document.getElementById("loadRoomsBtn");
    const connectBtn = document.getElementById("connectBtn");
    const sendBtn = document.getElementById("sendBtn");

    const roomWsAppDef = {
        data() {
            return {
                message: [],
                newMessage: "",
                stompClient: null,
                topicSubscription: null,
                roomEventSubscription: null,
                rooms: []
            };
        },
        created() {
            this.connectWebsocket();
        },
        beforeUnmount() {
            this.disconnectWebsocket();
        },
        methods: {
            log(message) {
                const now = new Date().toLocaleTimeString();
                const normalized = String(message)
                    .replace(/\\n/g, "\n")
                    .replace(/\r\n?/g, "\n");
                const lines = normalized.split("\n");

                lines.forEach((line, index) => {
                    if (index === lines.length - 1 && line === "") {
                        return;
                    }
                    logEl.textContent += "[" + now + "] " + line + "\n";
                });

                this.scrollToBottom();
            },
            serverBase() {
                return serverBaseInput.value.trim().replace(/\/+$/, "");
            },
            websocketUrl() {
                const base = new URL(this.serverBase());
                const wsProtocol = base.protocol === "https:" ? "wss:" : "ws:";
                return wsProtocol + "//" + base.host + "/ws";
            },
            async guestLogin() {
                const response = await fetch(this.serverBase() + "/api/v1/users/guest-login", {
                    method: "POST",
                    credentials: "include"
                });

                if (!response.ok) {
                    throw new Error("guest-login 실패: " + response.status);
                }

                const data = await response.json();
                const result = data.result || data.data || {};
                const userName = result.userName || result.nickname || "(없음)";
                this.log("guest-login 성공: userId=" + result.userId + ", userName=" + userName);
            },
            async fetchRooms() {
                const url = this.serverBase() + "/api/v1/rooms?page=0&size=20";
                const response = await fetch(url, {
                    method: "GET",
                    credentials: "include"
                });

                if (!response.ok) {
                    throw new Error("방 목록 조회 실패: " + response.status + " (guest-login 먼저 실행하세요)");
                }

                const data = await response.json();
                const result = data.result || {};
                const rooms = result.rooms || [];
                this.rooms = rooms;
                this.renderRooms();
                this.log("방 목록 조회 성공: " + rooms.length + "개");
            },
            async seedRooms() {
                const response = await fetch(this.serverBase() + "/api/v1/test/seed-rooms", {
                    method: "POST",
                    credentials: "include"
                });

                if (!response.ok) {
                    throw new Error("테스트 방 생성 실패: " + response.status + " (guest-login 먼저 실행하세요)");
                }

                const data = await response.json();
                const result = data.result || {};
                const rooms = result.rooms || [];
                this.log("테스트 데이터 생성 완료: users=" + (result.createdUserCount || 0) + ", rooms=" + rooms.length);

                await this.fetchRooms();
            },
            renderRooms() {
                if (!this.rooms.length) {
                    roomsViewEl.textContent = "조회된 방이 없습니다.";
                    return;
                }

                const lines = ["roomId | title | players | status | canJoin", "---------------------------------------"];
                this.rooms.forEach((room) => {
                    const line = [
                        room.roomId,
                        room.roomTitle,
                        room.playerCount + "/" + room.capacity,
                        room.status,
                        room.canJoin ? "Y" : "N"
                    ].join(" | ");
                    lines.push(line);
                });

                roomsViewEl.textContent = lines.join("\n");
            },
            async checkRoomAvailability(roomId) {
                const response = await fetch(this.serverBase() + "/api/v1/rooms/" + roomId + "/availability", {
                    method: "GET",
                    credentials: "include"
                });

                if (!response.ok) {
                    throw new Error("방 참가 가능 여부 확인 실패: " + response.status);
                }

                const data = await response.json();
                const result = data.result || {};
                return Boolean(result.canJoin);
            },
            async connectWebsocket() {
                if (this.stompClient && this.stompClient.connected) {
                    this.log("이미 연결됨");
                    return;
                }

                const roomId = roomIdInput.value.trim();
                if (!roomId) {
                    this.log("roomId를 입력하세요 (방 목록의 실제 roomId 사용)");
                    return;
                }

                try {
                    const canJoin = await this.checkRoomAvailability(roomId);
                    if (!canJoin) {
                        this.log("방 참가 불가: roomId=" + roomId);
                        return;
                    }
                } catch (error) {
                    this.log(error.message);
                    return;
                }

                const socket = new WebSocket(this.websocketUrl());
                this.stompClient = Stomp.over(socket);
                this.stompClient.debug = null;

                this.stompClient.connect({}, () => {
                    this.log("WS 연결 성공");

                    if (this.topicSubscription) {
                        this.topicSubscription.unsubscribe();
                    }
                    if (this.roomEventSubscription) {
                        this.roomEventSubscription.unsubscribe();
                    }

                    this.topicSubscription = this.stompClient.subscribe(`/topic/${roomId}`, (message) => {
                        this.message.push(message.body);
                        this.log(`수신 (/topic/${roomId}): ${message.body}`);
                    });

                    this.roomEventSubscription = this.stompClient.subscribe(`/topic/room/${roomId}`, (message) => {
                        this.log(`수신 (/topic/room/${roomId}): ${message.body}`);
                    });

                    this.stompClient.subscribe("/user/queue/errors", (frame) => {
                        this.log("에러 수신: " + frame.body);
                    });

                    this.stompClient.send("/app/room/join", {"content-type": "application/json"}, JSON.stringify({roomId: roomId}));

                    this.log(`구독 완료: /topic/${roomId}, /topic/room/${roomId}`);
                    this.log(`입장 요청 전송: /app/room/join (roomId=${roomId})`);
                }, (error) => {
                    this.log("WS 연결 실패: " + error);
                });
            },
            disconnectWebsocket() {
                if (this.topicSubscription) {
                    this.topicSubscription.unsubscribe();
                    this.topicSubscription = null;
                }

                if (this.roomEventSubscription) {
                    this.roomEventSubscription.unsubscribe();
                    this.roomEventSubscription = null;
                }

                if (this.stompClient && this.stompClient.connected) {
                    this.stompClient.disconnect(() => {
                        this.log("WS 연결 종료");
                    });
                }
            },
            sendMessage() {
                if (!this.stompClient || !this.stompClient.connected) {
                    this.log("먼저 connect 하세요");
                    return;
                }

                const roomId = roomIdInput.value.trim();
                this.newMessage = messageInput.value;

                if (!roomId) {
                    this.log("roomId를 입력하세요");
                    return;
                }

                if (this.newMessage.trim() === "") {
                    return;
                }

                this.stompClient.send(`/app/room/${roomId}`, {"content-type": "text/plain"}, this.newMessage);
                this.log(`전송 (/app/room/${roomId}): ${this.newMessage}`);
                this.newMessage = "";
                messageInput.value = "";
            },
            scrollToBottom() {
                const chatBox = document.querySelector(".chat-box") || logEl;
                if (!chatBox) {
                    return;
                }
                chatBox.scrollTop = chatBox.scrollHeight;
            }
        }
    };

    const app = roomWsAppDef.data();
    Object.entries(roomWsAppDef.methods).forEach(([name, fn]) => {
        app[name] = fn.bind(app);
    });
    app.created = roomWsAppDef.created.bind(app);
    app.beforeUnmount = roomWsAppDef.beforeUnmount.bind(app);

    loginBtn.addEventListener("click", async function () {
        try {
            await app.guestLogin();
        } catch (error) {
            app.log(error.message);
        }
    });

    seedRoomsBtn.addEventListener("click", async function () {
        try {
            await app.seedRooms();
        } catch (error) {
            app.log(error.message);
        }
    });

    loadRoomsBtn.addEventListener("click", async function () {
        try {
            await app.fetchRooms();
        } catch (error) {
            app.log(error.message);
        }
    });

    connectBtn.addEventListener("click", async function () {
        await app.connectWebsocket();
    });

    sendBtn.addEventListener("click", function () {
        app.sendMessage();
    });

    messageInput.addEventListener("keydown", function (event) {
        if (event.key === "Enter") {
            app.sendMessage();
        }
    });

    window.addEventListener("beforeunload", function () {
        app.beforeUnmount();
    });
})();
