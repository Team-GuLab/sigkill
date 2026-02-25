(function () {
    "use strict";

    const serverBaseInput = document.getElementById("serverBase");
    const wsUrlPreviewInput = document.getElementById("wsUrlPreview");

    const loginBtn = document.getElementById("loginBtn");
    const loadRoomsBtn = document.getElementById("loadRoomsBtn");
    const createRoomFlowBtn = document.getElementById("createRoomFlowBtn");
    const joinFlowBtn = document.getElementById("joinFlowBtn");
    const readyBtn = document.getElementById("readyBtn");
    const unreadyBtn = document.getElementById("unreadyBtn");
    const startBtn = document.getElementById("startBtn");
    const quizStartBtn = document.getElementById("quizStartBtn");
    const leaveDisconnectBtn = document.getElementById("leaveDisconnectBtn");
    const pingBtn = document.getElementById("pingBtn");
    const clearLogBtn = document.getElementById("clearLogBtn");
    const seedRoomsTopBtn = document.getElementById("seedRoomsTopBtn");

    const roomsPageInput = document.getElementById("roomsPage");
    const roomsSizeInput = document.getElementById("roomsSize");
    const createRoomTitleInput = document.getElementById("createRoomTitle");
    const createRoomCapacityInput = document.getElementById("createRoomCapacity");

    const joinRoomIdInput = document.getElementById("joinRoomId");
    const readyRoomIdInput = document.getElementById("readyRoomId");
    const startRoomIdInput = document.getElementById("startRoomId");
    const startQuizRoomIdInput = document.getElementById("startQuizRoomId");
    const startQuizGameIdInput = document.getElementById("startQuizGameId");
    const leaveRoomIdInput = document.getElementById("leaveRoomId");

    const roomTopicPreviewInput = document.getElementById("roomTopicPreview");
    const gameTopicPreviewInput = document.getElementById("gameTopicPreview");
    const errorTopicPreviewInput = document.getElementById("errorTopicPreview");
    const readyPayloadPreviewInput = document.getElementById("readyPayloadPreview");
    const startPayloadPreviewInput = document.getElementById("startPayloadPreview");
    const quizStartDestinationInput = document.getElementById("quizStartDestination");
    const quizStartPayloadPreviewInput = document.getElementById("quizStartPayloadPreview");
    const leavePayloadPreviewInput = document.getElementById("leavePayloadPreview");

    const loginResponseEl = document.getElementById("loginResponse");
    const roomsResponseEl = document.getElementById("roomsResponse");
    const joinRequestEl = document.getElementById("joinRequest");
    const joinResponseEl = document.getElementById("joinResponse");
    const joinSendEl = document.getElementById("joinSend");
    const createRequestEl = document.getElementById("createRequest");
    const createResponseEl = document.getElementById("createResponse");
    const createWsResultEl = document.getElementById("createWsResult");
    const readyResponseEl = document.getElementById("readyResponse");
    const startResponseEl = document.getElementById("startResponse");
    const quizStartResponseEl = document.getElementById("quizStartResponse");
    const leaveResponseEl = document.getElementById("leaveResponse");

    const roomEventPanelEl = document.getElementById("roomEventPanel");
    const gameEventPanelEl = document.getElementById("gameEventPanel");
    const errorEventPanelEl = document.getElementById("errorEventPanel");
    const pongEventPanelEl = document.getElementById("pongEventPanel");
    const logEl = document.getElementById("log");

    const state = {
        stompClient: null,
        roomSubscription: null,
        gameSubscription: null,
        errorSubscription: null,
        pongSubscription: null,
        subscribedRoomId: null,
        subscribedGameId: null
    };

    function initialize() {
        const origin = window.location.origin || "http://localhost:8080";
        serverBaseInput.value = origin;
        errorTopicPreviewInput.value = "/user/queue/errors";
        refreshComputedFields();
        logEl.textContent = "";
    }

    function now() {
        return new Date().toLocaleTimeString();
    }

    function log(message) {
        logEl.textContent += "[" + now() + "] " + message + "\n";
        logEl.scrollTop = logEl.scrollHeight;
    }

    function parseJsonSafe(raw) {
        if (!raw) {
            return null;
        }
        try {
            return JSON.parse(raw);
        } catch (error) {
            return null;
        }
    }

    function toPretty(value) {
        if (value === null || value === undefined) {
            return "-";
        }
        if (typeof value === "string") {
            return value;
        }
        return JSON.stringify(value, null, 2);
    }

    function setPanel(el, value) {
        el.textContent = toPretty(value);
    }

    function getResponseBodyForPanel(response) {
        if (response.body !== null && response.body !== undefined) {
            return response.body;
        }
        return response.rawText || "";
    }

    function getServerBase() {
        const value = serverBaseInput.value.trim().replace(/\/+$/, "");
        if (!value) {
            throw new Error("serverBase를 입력하세요.");
        }
        return value;
    }

    function toWsUrl(baseUrl) {
        const url = new URL(baseUrl);
        const protocol = url.protocol === "https:" ? "wss:" : "ws:";
        return protocol + "//" + url.host + "/ws";
    }

    function getValueOrThrow(input, fieldName) {
        const value = input.value.trim();
        if (!value) {
            throw new Error(fieldName + " 값을 입력하세요.");
        }
        return value;
    }

    function normalizeRoomIdAcrossSections(sourceInput) {
        const roomId = sourceInput.value.trim();
        if (!roomId) {
            refreshComputedFields();
            return;
        }
        if (sourceInput !== joinRoomIdInput) {
            joinRoomIdInput.value = roomId;
        }
        if (sourceInput !== readyRoomIdInput) {
            readyRoomIdInput.value = roomId;
        }
        if (sourceInput !== startRoomIdInput) {
            startRoomIdInput.value = roomId;
        }
        if (sourceInput !== startQuizRoomIdInput) {
            startQuizRoomIdInput.value = roomId;
        }
        if (sourceInput !== leaveRoomIdInput) {
            leaveRoomIdInput.value = roomId;
        }
        refreshComputedFields();
    }

    function refreshComputedFields() {
        let wsUrl = "";
        try {
            wsUrl = toWsUrl(getServerBase());
        } catch (error) {
            wsUrl = "";
        }
        wsUrlPreviewInput.value = wsUrl;

        const joinRoomId = joinRoomIdInput.value.trim();
        roomTopicPreviewInput.value = joinRoomId ? "/topic/room/" + joinRoomId : "/topic/room/{roomId}";

        const readyRoomId = readyRoomIdInput.value.trim();
        readyPayloadPreviewInput.value = readyRoomId ? JSON.stringify({roomId: readyRoomId}) : "{\"roomId\":\"{roomId}\"}";

        const startRoomId = startRoomIdInput.value.trim();
        startPayloadPreviewInput.value = startRoomId ? JSON.stringify({roomId: startRoomId}) : "{\"roomId\":\"{roomId}\"}";

        const startQuizRoomId = startQuizRoomIdInput.value.trim();
        const startQuizGameId = startQuizGameIdInput.value.trim();
        gameTopicPreviewInput.value = startQuizGameId ? "/topic/game/" + startQuizGameId : "/topic/game/{gameId}";
        quizStartDestinationInput.value = "AUTO (server orchestrator)";
        quizStartPayloadPreviewInput.value = startQuizRoomId && startQuizGameId
            ? "no client SEND (server-managed)"
            : "gameId가 정해지면 서버가 자동 진행";

        const leaveRoomId = leaveRoomIdInput.value.trim();
        leavePayloadPreviewInput.value = leaveRoomId ? JSON.stringify({roomId: leaveRoomId}) : "{\"roomId\":\"{roomId}\"}";
    }

    async function fetchJson(path, options) {
        const response = await fetch(getServerBase() + path, options);
        const rawText = await response.text();
        const body = parseJsonSafe(rawText);

        return {
            ok: response.ok,
            status: response.status,
            body: body,
            rawText: rawText
        };
    }

    async function executeGuestLogin() {
        const response = await fetchJson("/api/v1/users/guest-login", {
            method: "POST",
            credentials: "include"
        });

        setPanel(loginResponseEl, getResponseBodyForPanel(response));
        if (!response.ok) {
            throw new Error("게스트 로그인 실패: HTTP " + response.status);
        }
        log("1) 게스트 로그인 성공");
    }

    async function executeLoadRooms() {
        const page = Number(roomsPageInput.value);
        const size = Number(roomsSizeInput.value);

        const response = await fetchJson("/api/v1/rooms?page=" + page + "&size=" + size, {
            method: "GET",
            credentials: "include"
        });

        setPanel(roomsResponseEl, getResponseBodyForPanel(response));
        if (!response.ok) {
            throw new Error("방 목록 조회 실패: HTTP " + response.status);
        }

        log("2) 방 목록 조회 성공");
    }

    function extractRoomIdFromCreateResponse(body) {
        if (!body || !body.result) {
            return null;
        }

        if (body.result.room && body.result.room.roomId) {
            return String(body.result.room.roomId);
        }

        if (body.result.roomId) {
            return String(body.result.roomId);
        }

        return null;
    }

    function syncRoomIdToAllInputs(roomId) {
        joinRoomIdInput.value = roomId;
        readyRoomIdInput.value = roomId;
        startRoomIdInput.value = roomId;
        startQuizRoomIdInput.value = roomId;
        leaveRoomIdInput.value = roomId;
        refreshComputedFields();
    }

    async function executeCreateRoomFlow() {
        const roomTitle = getValueOrThrow(createRoomTitleInput, "roomTitle");
        const capacityRaw = createRoomCapacityInput.value.trim();
        const payload = {roomTitle: roomTitle};

        if (capacityRaw) {
            const parsedCapacity = Number(capacityRaw);
            if (!Number.isInteger(parsedCapacity)) {
                throw new Error("capacity는 정수여야 합니다.");
            }
            payload.capacity = parsedCapacity;
        }

        setPanel(createRequestEl, {
            method: "POST",
            path: "/api/v1/rooms",
            body: payload
        });
        setPanel(createWsResultEl, "-");

        const response = await fetchJson("/api/v1/rooms", {
            method: "POST",
            credentials: "include",
            headers: {"content-type": "application/json"},
            body: JSON.stringify(payload)
        });

        setPanel(createResponseEl, getResponseBodyForPanel(response));
        if (!response.ok) {
            throw new Error("방 생성 실패: HTTP " + response.status);
        }

        const createdRoomId = extractRoomIdFromCreateResponse(response.body);
        if (!createdRoomId) {
            throw new Error("방 생성 응답에서 roomId를 찾을 수 없습니다.");
        }

        syncRoomIdToAllInputs(createdRoomId);

        const connectResult = await connectStompIfNeeded();
        const errorSubscribeResult = subscribeErrorQueueOnce();
        const pongSubscribeResult = subscribePongQueueOnce();
        const roomSubscribeResult = subscribeRoomTopic(createdRoomId);

        setPanel(createWsResultEl, {
            wsConnected: connectResult,
            errorSubscription: errorSubscribeResult,
            pongSubscription: pongSubscribeResult,
            roomSubscription: roomSubscribeResult,
            autoFilledRoomId: createdRoomId
        });

        log("3) 방 생성 통합 실행 완료 (roomId=" + createdRoomId + ")");
    }

    async function executeSeedRoomsTop() {
        const response = await fetchJson("/api/v1/test/seed-rooms", {
            method: "POST",
            credentials: "include"
        });

        setPanel(roomsResponseEl, getResponseBodyForPanel(response));
        if (!response.ok) {
            throw new Error("테스트 데이터 생성 실패: HTTP " + response.status);
        }

        const result = response.body && response.body.result ? response.body.result : {};
        log("테스트 데이터 생성 성공: users=" + String(result.createdUserCount) + ", rooms=" +
            String(Array.isArray(result.rooms) ? result.rooms.length : 0));

        await executeLoadRooms();
    }

    async function connectStompIfNeeded() {
        if (state.stompClient && state.stompClient.connected) {
            return {connected: true, newlyConnected: false};
        }

        const socket = new WebSocket(toWsUrl(getServerBase()));
        state.stompClient = Stomp.over(socket);
        state.stompClient.debug = null;
        state.stompClient.heartbeat.outgoing = 10000;
        state.stompClient.heartbeat.incoming = 10000;
        state.stompClient.reconnect_delay = 5000;

        await new Promise((resolve, reject) => {
            state.stompClient.connect({}, () => resolve(), (error) => reject(error));
        });

        return {connected: true, newlyConnected: true};
    }

    function subscribeErrorQueueOnce() {
        if (state.errorSubscription) {
            return {subscribed: true, newlySubscribed: false};
        }

        state.errorSubscription = state.stompClient.subscribe("/user/queue/errors", (frame) => {
            const parsed = parseJsonSafe(frame.body);
            setPanel(errorEventPanelEl, parsed || frame.body);
            log("에러 이벤트 수신");
        });

        return {subscribed: true, newlySubscribed: true};
    }

    function subscribePongQueueOnce() {
        if (state.pongSubscription) {
            return {subscribed: true, newlySubscribed: false};
        }

        state.pongSubscription = state.stompClient.subscribe("/user/queue/pong", (frame) => {
            const parsed = parseJsonSafe(frame.body);
            setPanel(pongEventPanelEl, parsed || frame.body);
            log("PONG 이벤트 수신");
        });

        return {subscribed: true, newlySubscribed: true};
    }

    function subscribeRoomTopic(roomId) {
        if (state.roomSubscription) {
            state.roomSubscription.unsubscribe();
            state.roomSubscription = null;
        }

        state.roomSubscription = state.stompClient.subscribe("/topic/room/" + roomId, (frame) => {
            const parsed = parseJsonSafe(frame.body);
            setPanel(roomEventPanelEl, parsed || frame.body);
            log("Room 이벤트 수신");

            if (parsed && parsed.type === "GAME_START" && parsed.gameId !== null && parsed.gameId !== undefined) {
                startQuizGameIdInput.value = String(parsed.gameId);
                if (parsed.roomId) {
                    startQuizRoomIdInput.value = String(parsed.roomId);
                    normalizeRoomIdAcrossSections(startQuizRoomIdInput);
                } else {
                    refreshComputedFields();
                }

                const gameSubscribeResult = subscribeGameTopic(String(parsed.gameId));
                log("GAME_START 감지: gameId 자동 반영 및 Game 토픽 구독 (" + gameSubscribeResult.topic + ")");
            }
        });

        state.subscribedRoomId = roomId;
        return {subscribed: true, topic: "/topic/room/" + roomId};
    }

    function subscribeGameTopic(gameId) {
        if (state.gameSubscription) {
            state.gameSubscription.unsubscribe();
            state.gameSubscription = null;
        }

        state.gameSubscription = state.stompClient.subscribe("/topic/game/" + gameId, (frame) => {
            const parsed = parseJsonSafe(frame.body);
            setPanel(gameEventPanelEl, parsed || frame.body);
            log("Game 이벤트 수신");
        });

        state.subscribedGameId = String(gameId);
        return {subscribed: true, topic: "/topic/game/" + gameId};
    }

    function sendRoomCommand(command, roomId) {
        if (!state.stompClient || !state.stompClient.connected) {
            throw new Error("WebSocket 연결이 필요합니다.");
        }

        const destination = "/app/room/" + command;
        const payload = JSON.stringify({roomId: roomId});
        state.stompClient.send(destination, {"content-type": "application/json"}, payload);

        return {
            destination: destination,
            payload: {roomId: roomId}
        };
    }

    function sendGameCommand(destination, payload) {
        if (!state.stompClient || !state.stompClient.connected) {
            throw new Error("WebSocket 연결이 필요합니다.");
        }

        const payloadJson = JSON.stringify(payload);
        state.stompClient.send(destination, {"content-type": "application/json"}, payloadJson);

        return {
            destination: destination,
            payload: payload
        };
    }

    async function executeJoinFlow() {
        normalizeRoomIdAcrossSections(joinRoomIdInput);
        const roomId = getValueOrThrow(joinRoomIdInput, "roomId");
        const availabilityPath = "/api/v1/rooms/" + encodeURIComponent(roomId) + "/availability";

        setPanel(joinRequestEl, {
            method: "GET",
            path: availabilityPath
        });
        setPanel(joinSendEl, "-");

        const availabilityResponse = await fetchJson(availabilityPath, {
            method: "GET",
            credentials: "include"
        });
        setPanel(joinResponseEl, getResponseBodyForPanel(availabilityResponse));

        if (!availabilityResponse.ok) {
            throw new Error("방 참가 가능 여부 조회 실패: HTTP " + availabilityResponse.status);
        }

        const canJoin = Boolean(
            availabilityResponse.body &&
            availabilityResponse.body.result &&
            availabilityResponse.body.result.canJoin
        );
        if (!canJoin) {
            throw new Error("참가 불가 상태입니다. canJoin=false");
        }

        await connectStompIfNeeded();
        subscribeErrorQueueOnce();
        subscribePongQueueOnce();
        subscribeRoomTopic(roomId);
        const sendResult = sendRoomCommand("join", roomId);
        setPanel(joinSendEl, sendResult);
        log("4) 방 참가 통합 실행 완료");
    }

    async function executePing() {
        await connectStompIfNeeded();
        subscribeErrorQueueOnce();
        subscribePongQueueOnce();
        state.stompClient.send("/app/ping", {"content-type": "application/json"}, "{}");
        log("PING 전송 완료");
    }

    function executeReadyLike(command) {
        const roomId = getValueOrThrow(readyRoomIdInput, "roomId");
        normalizeRoomIdAcrossSections(readyRoomIdInput);
        const sendResult = sendRoomCommand(command, roomId);
        setPanel(readyResponseEl, sendResult);
        log("5) " + command.toUpperCase() + " 전송 완료");
    }

    function executeStartGame() {
        const roomId = getValueOrThrow(startRoomIdInput, "roomId");
        normalizeRoomIdAcrossSections(startRoomIdInput);
        const sendResult = sendRoomCommand("start", roomId);
        setPanel(startResponseEl, sendResult);
        log("6) START 전송 완료");
    }

    async function executeStartQuiz() {
        const gameIdRaw = getValueOrThrow(startQuizGameIdInput, "gameId");
        const gameId = Number(gameIdRaw);
        if (!Number.isFinite(gameId)) {
            throw new Error("gameId는 숫자여야 합니다.");
        }

        normalizeRoomIdAcrossSections(startQuizRoomIdInput);
        await connectStompIfNeeded();
        subscribeErrorQueueOnce();
        subscribePongQueueOnce();
        const gameSubscribeResult = subscribeGameTopic(String(gameId));
        setPanel(quizStartResponseEl, {
            action: "SUBSCRIBE_ONLY",
            topic: gameSubscribeResult.topic,
            note: "퀴즈는 GAME_START 이후 서버가 자동 진행합니다."
        });
        log("7) GAME 토픽 구독 완료 (퀴즈 자동 진행)");
    }

    function disconnectStomp() {
        if (state.roomSubscription) {
            state.roomSubscription.unsubscribe();
            state.roomSubscription = null;
        }
        if (state.errorSubscription) {
            state.errorSubscription.unsubscribe();
            state.errorSubscription = null;
        }
        if (state.gameSubscription) {
            state.gameSubscription.unsubscribe();
            state.gameSubscription = null;
        }
        if (state.pongSubscription) {
            state.pongSubscription.unsubscribe();
            state.pongSubscription = null;
        }

        if (state.stompClient && state.stompClient.connected) {
            state.stompClient.disconnect(() => {
                log("WebSocket 연결 종료");
            });
        }

        state.subscribedRoomId = null;
        state.subscribedGameId = null;
    }

    async function executeLeaveAndDisconnect() {
        const roomId = getValueOrThrow(leaveRoomIdInput, "roomId");
        normalizeRoomIdAcrossSections(leaveRoomIdInput);

        if (state.stompClient && state.stompClient.connected) {
            const sendResult = sendRoomCommand("leave", roomId);
            setPanel(leaveResponseEl, sendResult);
            await new Promise((resolve) => setTimeout(resolve, 150));
            disconnectStomp();
            log("8) LEAVE 전송 + WS 연결 해제 완료");
        } else {
            log("8) WS 미연결 상태로 연결해제 단계만 스킵");
        }
    }

    function bindAsync(button, handler) {
        button.addEventListener("click", async () => {
            try {
                await handler();
            } catch (error) {
                log("오류: " + (error && error.message ? error.message : String(error)));
            }
        });
    }

    serverBaseInput.addEventListener("input", refreshComputedFields);
    joinRoomIdInput.addEventListener("input", () => normalizeRoomIdAcrossSections(joinRoomIdInput));
    readyRoomIdInput.addEventListener("input", () => normalizeRoomIdAcrossSections(readyRoomIdInput));
    startRoomIdInput.addEventListener("input", () => normalizeRoomIdAcrossSections(startRoomIdInput));
    startQuizRoomIdInput.addEventListener("input", () => normalizeRoomIdAcrossSections(startQuizRoomIdInput));
    startQuizGameIdInput.addEventListener("input", refreshComputedFields);
    leaveRoomIdInput.addEventListener("input", () => normalizeRoomIdAcrossSections(leaveRoomIdInput));

    bindAsync(loginBtn, executeGuestLogin);
    bindAsync(loadRoomsBtn, executeLoadRooms);
    bindAsync(createRoomFlowBtn, executeCreateRoomFlow);
    bindAsync(joinFlowBtn, executeJoinFlow);
    bindAsync(readyBtn, async () => executeReadyLike("ready"));
    bindAsync(unreadyBtn, async () => executeReadyLike("unready"));
    bindAsync(startBtn, async () => executeStartGame());
    bindAsync(quizStartBtn, executeStartQuiz);
    bindAsync(leaveDisconnectBtn, executeLeaveAndDisconnect);
    bindAsync(pingBtn, executePing);
    bindAsync(seedRoomsTopBtn, executeSeedRoomsTop);

    clearLogBtn.addEventListener("click", () => {
        logEl.textContent = "";
    });

    window.addEventListener("beforeunload", disconnectStomp);

    initialize();
})();
