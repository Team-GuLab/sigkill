(function () {
    "use strict";

    const serverBaseInput = document.getElementById("serverBase");
    const wsUrlPreviewInput = document.getElementById("wsUrlPreview");

    const loginBtn = document.getElementById("loginBtn");
    const loadRoomsBtn = document.getElementById("loadRoomsBtn");
    const createRoomFlowBtn = document.getElementById("createRoomFlowBtn");
    const joinFlowBtn = document.getElementById("joinFlowBtn");
    const snapshotBtn = document.getElementById("snapshotBtn");
    const readyBtn = document.getElementById("readyBtn");
    const unreadyBtn = document.getElementById("unreadyBtn");
    const botBtn = document.getElementById("botBtn");
    const startBtn = document.getElementById("startBtn");
    const gameLoadBtn = document.getElementById("gameLoadBtn");
    const submitChoiceBtn = document.getElementById("submitChoiceBtn");
    const leaveDisconnectBtn = document.getElementById("leaveDisconnectBtn");
    const pingBtn = document.getElementById("pingBtn");
    const clearLogBtn = document.getElementById("clearLogBtn");
    const seedRoomsTopBtn = document.getElementById("seedRoomsTopBtn");

    const roomsPageInput = document.getElementById("roomsPage");
    const roomsSizeInput = document.getElementById("roomsSize");
    const createRoomTitleInput = document.getElementById("createRoomTitle");
    const createRoomCapacityInput = document.getElementById("createRoomCapacity");

    const joinRoomIdInput = document.getElementById("joinRoomId");
    const snapshotRoomIdInput = document.getElementById("snapshotRoomId");
    const readyRoomIdInput = document.getElementById("readyRoomId");
    const botRoomIdInput = document.getElementById("botRoomId");
    const startRoomIdInput = document.getElementById("startRoomId");
    const startQuizRoomIdInput = document.getElementById("startQuizRoomId");
    const startQuizGameIdInput = document.getElementById("startQuizGameId");
    const submitGameIdInput = document.getElementById("submitGameId");
    const submitQuizIdInput = document.getElementById("submitQuizId");
    const submitChoiceNumberInput = document.getElementById("submitChoiceNumber");
    const leaveRoomIdInput = document.getElementById("leaveRoomId");

    const roomTopicPreviewInput = document.getElementById("roomTopicPreview");
    const gameTopicPreviewInput = document.getElementById("gameTopicPreview");
    const errorTopicPreviewInput = document.getElementById("errorTopicPreview");
    const snapshotPayloadPreviewInput = document.getElementById("snapshotPayloadPreview");
    const readyPayloadPreviewInput = document.getElementById("readyPayloadPreview");
    const botPayloadPreviewInput = document.getElementById("botPayloadPreview");
    const startPayloadPreviewInput = document.getElementById("startPayloadPreview");
    const quizStartDestinationInput = document.getElementById("quizStartDestination");
    const quizStartPayloadPreviewInput = document.getElementById("quizStartPayloadPreview");
    const gameLoadPayloadPreviewInput = document.getElementById("gameLoadPayloadPreview");
    const submitPayloadPreviewInput = document.getElementById("submitPayloadPreview");
    const leavePayloadPreviewInput = document.getElementById("leavePayloadPreview");

    const loginResponseEl = document.getElementById("loginResponse");
    const roomsResponseEl = document.getElementById("roomsResponse");
    const joinRequestEl = document.getElementById("joinRequest");
    const joinResponseEl = document.getElementById("joinResponse");
    const joinSendEl = document.getElementById("joinSend");
    const createRequestEl = document.getElementById("createRequest");
    const createResponseEl = document.getElementById("createResponse");
    const createWsResultEl = document.getElementById("createWsResult");
    const snapshotResponseEl = document.getElementById("snapshotResponse");
    const readyResponseEl = document.getElementById("readyResponse");
    const botResponseEl = document.getElementById("botResponse");
    const startResponseEl = document.getElementById("startResponse");
    const quizStartResponseEl = document.getElementById("quizStartResponse");
    const gameLoadResponseEl = document.getElementById("gameLoadResponse");
    const submitResponseEl = document.getElementById("submitResponse");
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
        setPanel(quizStartResponseEl, {
            mode: "AUTO_ON_GAME_START",
            status: "WAITING_FOR_GAME_START"
        });
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

    function resolveMessageType(parsed, fallbackType) {
        if (parsed && typeof parsed === "object" && parsed.type !== null && parsed.type !== undefined) {
            return String(parsed.type);
        }
        return fallbackType;
    }

    function logInbound(endpoint, parsed, fallbackType) {
        const type = resolveMessageType(parsed, fallbackType);
        log("수신 응답: type=" + type + ", endpoint=" + endpoint);
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
        if (sourceInput !== snapshotRoomIdInput) {
            snapshotRoomIdInput.value = roomId;
        }
        if (sourceInput !== readyRoomIdInput) {
            readyRoomIdInput.value = roomId;
        }
        if (sourceInput !== botRoomIdInput) {
            botRoomIdInput.value = roomId;
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

        const snapshotRoomId = snapshotRoomIdInput.value.trim();
        snapshotPayloadPreviewInput.value = snapshotRoomId
            ? JSON.stringify({roomId: snapshotRoomId})
            : "{\"roomId\":\"{roomId}\"}";

        const readyRoomId = readyRoomIdInput.value.trim();
        readyPayloadPreviewInput.value = readyRoomId ? JSON.stringify({roomId: readyRoomId}) : "{\"roomId\":\"{roomId}\"}";

        const botRoomId = botRoomIdInput.value.trim();
        botPayloadPreviewInput.value = botRoomId ? JSON.stringify({roomId: botRoomId}) : "{\"roomId\":\"{roomId}\"}";

        const startRoomId = startRoomIdInput.value.trim();
        startPayloadPreviewInput.value = startRoomId ? JSON.stringify({roomId: startRoomId}) : "{\"roomId\":\"{roomId}\"}";

        const startQuizRoomId = startQuizRoomIdInput.value.trim();
        const startQuizGameId = startQuizGameIdInput.value.trim();
        gameTopicPreviewInput.value = startQuizGameId ? "/topic/game/" + startQuizGameId : "/topic/game/{gameId}";
        quizStartDestinationInput.value = "AUTO (server orchestrator)";
        quizStartPayloadPreviewInput.value = startQuizRoomId
            ? "GAME_START 수신 시 자동 구독"
            : "방 참가 후 GAME_START 수신 대기";
        gameLoadPayloadPreviewInput.value = startQuizGameId
            ? JSON.stringify({gameId: Number(startQuizGameId)})
            : "{\"gameId\":{gameId}}";

        const submitGameId = submitGameIdInput.value.trim();
        const submitQuizId = submitQuizIdInput.value.trim();
        const submitChoiceNumber = submitChoiceNumberInput.value.trim();
        submitPayloadPreviewInput.value = submitGameId && submitQuizId && submitChoiceNumber
            ? JSON.stringify({
                gameId: Number(submitGameId),
                quizId: Number(submitQuizId),
                choiceNumber: Number(submitChoiceNumber)
            })
            : "{\"gameId\":{gameId},\"quizId\":{quizId},\"choiceNumber\":{choiceNumber}}";

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
        snapshotRoomIdInput.value = roomId;
        readyRoomIdInput.value = roomId;
        botRoomIdInput.value = roomId;
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
        const roomSnapshotSendResult = sendRoomCommand("snapshot", createdRoomId);

        setPanel(createWsResultEl, {
            wsConnected: connectResult,
            errorSubscription: errorSubscribeResult,
            pongSubscription: pongSubscribeResult,
            roomSubscription: roomSubscribeResult,
            roomSnapshotSend: roomSnapshotSendResult,
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

        const endpoint = "/user/queue/errors";
        state.errorSubscription = state.stompClient.subscribe(endpoint, (frame) => {
            const parsed = parseJsonSafe(frame.body);
            setPanel(errorEventPanelEl, parsed || frame.body);
            logInbound(endpoint, parsed, "ERROR");
        });

        return {subscribed: true, newlySubscribed: true};
    }

    function subscribePongQueueOnce() {
        if (state.pongSubscription) {
            return {subscribed: true, newlySubscribed: false};
        }

        const endpoint = "/user/queue/pong";
        state.pongSubscription = state.stompClient.subscribe(endpoint, (frame) => {
            const parsed = parseJsonSafe(frame.body);
            setPanel(pongEventPanelEl, parsed || frame.body);
            logInbound(endpoint, parsed, "PONG");
        });

        return {subscribed: true, newlySubscribed: true};
    }

    function subscribeRoomTopic(roomId) {
        if (state.roomSubscription) {
            state.roomSubscription.unsubscribe();
            state.roomSubscription = null;
        }

        const endpoint = "/topic/room/" + roomId;
        state.roomSubscription = state.stompClient.subscribe(endpoint, (frame) => {
            const parsed = parseJsonSafe(frame.body);
            setPanel(roomEventPanelEl, parsed || frame.body);
            logInbound(endpoint, parsed, "UNKNOWN");

            if (parsed && parsed.type === "GAME_START" && parsed.gameId !== null && parsed.gameId !== undefined) {
                const gameId = String(parsed.gameId);
                startQuizGameIdInput.value = gameId;
                submitGameIdInput.value = gameId;
                if (parsed.roomId) {
                    startQuizRoomIdInput.value = String(parsed.roomId);
                    normalizeRoomIdAcrossSections(startQuizRoomIdInput);
                } else {
                    refreshComputedFields();
                }

                const gameSubscribeResult = subscribeGameTopic(gameId);
                setPanel(quizStartResponseEl, {
                    trigger: "GAME_START",
                    roomId: parsed.roomId ? String(parsed.roomId) : state.subscribedRoomId,
                    gameId: gameId,
                    topic: gameSubscribeResult.topic,
                    newlySubscribed: gameSubscribeResult.newlySubscribed
                });
                log("GAME_START 감지: gameId 자동 반영 및 Game 토픽 자동 구독 (" + gameSubscribeResult.topic + ")");
            }
        });

        state.subscribedRoomId = roomId;
        return {subscribed: true, topic: "/topic/room/" + roomId};
    }

    function subscribeGameTopic(gameId) {
        const normalizedGameId = String(gameId);
        if (state.gameSubscription && state.subscribedGameId === normalizedGameId) {
            return {subscribed: true, newlySubscribed: false, topic: "/topic/game/" + normalizedGameId};
        }

        if (state.gameSubscription) {
            state.gameSubscription.unsubscribe();
            state.gameSubscription = null;
        }

        const endpoint = "/topic/game/" + normalizedGameId;
        state.gameSubscription = state.stompClient.subscribe(endpoint, (frame) => {
            const parsed = parseJsonSafe(frame.body);
            setPanel(gameEventPanelEl, parsed || frame.body);
            logInbound(endpoint, parsed, "UNKNOWN");

            const quizId = parsed && parsed.payload && parsed.payload.quiz ? parsed.payload.quiz.quizId : null;
            if (parsed && parsed.type === "QUIZ_START" && quizId !== null && quizId !== undefined) {
                submitGameIdInput.value = normalizedGameId;
                submitQuizIdInput.value = String(quizId);
                refreshComputedFields();
                log("QUIZ_START 감지: 제출 폼 자동 반영 (quizId=" + String(quizId) + ")");
            }
        });

        state.subscribedGameId = normalizedGameId;
        return {subscribed: true, newlySubscribed: true, topic: "/topic/game/" + normalizedGameId};
    }

    function sendRoomCommand(command, roomId) {
        return sendRoomCommandWithPayload(command, {roomId: roomId});
    }

    function sendRoomCommandWithPayload(command, payload) {
        if (!state.stompClient || !state.stompClient.connected) {
            throw new Error("WebSocket 연결이 필요합니다.");
        }

        const destination = "/app/room/" + command;
        const payloadJson = JSON.stringify(payload);
        state.stompClient.send(destination, {"content-type": "application/json"}, payloadJson);

        return {
            destination: destination,
            payload: payload
        };
    }

    function sendSubmitChoiceCommand(gameId, quizId, choiceNumber) {
        if (!state.stompClient || !state.stompClient.connected) {
            throw new Error("WebSocket 연결이 필요합니다.");
        }

        const destination = "/app/game/submit";
        const payload = {
            gameId: gameId,
            quizId: quizId,
            choiceNumber: choiceNumber
        };
        state.stompClient.send(destination, {"content-type": "application/json"}, JSON.stringify(payload));

        return {
            destination: destination,
            payload: payload
        };
    }

    function sendGameLoadCommand(gameId) {
        if (!state.stompClient || !state.stompClient.connected) {
            throw new Error("WebSocket 연결이 필요합니다.");
        }

        const destination = "/app/game/load";
        const payload = {gameId: gameId};
        state.stompClient.send(destination, {"content-type": "application/json"}, JSON.stringify(payload));

        return {
            destination: destination,
            payload: payload
        };
    }

    async function executeJoinFlow() {
        normalizeRoomIdAcrossSections(joinRoomIdInput);
        const roomId = getValueOrThrow(joinRoomIdInput, "roomId");
        const joinPath = "/api/v1/rooms/" + encodeURIComponent(roomId) + "/join";

        setPanel(joinRequestEl, {
            method: "POST",
            path: joinPath
        });
        setPanel(joinSendEl, "-");

        const joinResponse = await fetchJson(joinPath, {
            method: "POST",
            credentials: "include"
        });
        setPanel(joinResponseEl, getResponseBodyForPanel(joinResponse));

        if (!joinResponse.ok) {
            throw new Error("방 참가 실패: HTTP " + joinResponse.status);
        }

        await connectStompIfNeeded();
        subscribeErrorQueueOnce();
        subscribePongQueueOnce();
        subscribeRoomTopic(roomId);

        const roomJoinSendResult = sendRoomCommand("join", roomId);
        const roomSnapshotSendResult = sendRoomCommand("snapshot", roomId);
        setPanel(joinSendEl, {
            roomJoinSend: roomJoinSendResult,
            roomSnapshotSend: roomSnapshotSendResult
        });
        log("4) 방 참가 통합 실행 완료 (rest join + ws join + ws snapshot)");
    }

    async function executePing() {
        await connectStompIfNeeded();
        subscribeErrorQueueOnce();
        subscribePongQueueOnce();
        state.stompClient.send("/app/ping", {"content-type": "application/json"}, "{}");
        log("PING 전송 완료");
    }

    async function executeSnapshot() {
        const roomId = getValueOrThrow(snapshotRoomIdInput, "roomId");
        normalizeRoomIdAcrossSections(snapshotRoomIdInput);

        await connectStompIfNeeded();
        subscribeErrorQueueOnce();
        subscribePongQueueOnce();
        const roomSubscribeResult = subscribeRoomTopic(roomId);
        const sendResult = sendRoomCommand("snapshot", roomId);

        setPanel(snapshotResponseEl, {
            ...sendResult,
            roomSubscription: roomSubscribeResult
        });
        log("5) SNAPSHOT 전송 완료");
    }

    function executeReadyLike(command) {
        const roomId = getValueOrThrow(readyRoomIdInput, "roomId");
        normalizeRoomIdAcrossSections(readyRoomIdInput);
        const sendResult = sendRoomCommand(command, roomId);
        setPanel(readyResponseEl, sendResult);
        log("6) " + command.toUpperCase() + " 전송 완료");
    }

    async function executeAddBot() {
        const roomId = getValueOrThrow(botRoomIdInput, "roomId");
        normalizeRoomIdAcrossSections(botRoomIdInput);

        await connectStompIfNeeded();
        subscribeErrorQueueOnce();
        subscribePongQueueOnce();
        const roomSubscribeResult = subscribeRoomTopic(roomId);
        const sendResult = sendRoomCommand("bot", roomId);

        setPanel(botResponseEl, {
            ...sendResult,
            roomSubscription: roomSubscribeResult,
            expectedEvents: ["PLAYER_JOIN", "PLAYER_READY"]
        });
        log("7) BOT 추가 전송 완료");
    }

    function executeStartGame() {
        const roomId = getValueOrThrow(startRoomIdInput, "roomId");
        normalizeRoomIdAcrossSections(startRoomIdInput);
        const sendResult = sendRoomCommand("start", roomId);
        setPanel(startResponseEl, sendResult);
        log("8) START 전송 완료");
    }

    async function executeGameLoad() {
        const gameId = Number(getValueOrThrow(startQuizGameIdInput, "gameId"));
        if (!Number.isInteger(gameId) || gameId <= 0) {
            throw new Error("gameId는 1 이상의 정수여야 합니다.");
        }

        await connectStompIfNeeded();
        subscribeErrorQueueOnce();
        subscribePongQueueOnce();
        const gameSubscribeResult = subscribeGameTopic(String(gameId));
        const sendResult = sendGameLoadCommand(gameId);

        setPanel(gameLoadResponseEl, {
            ...sendResult,
            gameSubscription: gameSubscribeResult
        });
        log("9) GAME_LOAD 전송 완료");
    }

    async function executeSubmitChoice() {
        const gameId = Number(getValueOrThrow(submitGameIdInput, "gameId"));
        const quizId = Number(getValueOrThrow(submitQuizIdInput, "quizId"));
        const choiceNumber = Number(getValueOrThrow(submitChoiceNumberInput, "choiceNumber"));

        if (!Number.isInteger(gameId) || gameId <= 0) {
            throw new Error("gameId는 1 이상의 정수여야 합니다.");
        }
        if (!Number.isInteger(quizId) || quizId <= 0) {
            throw new Error("quizId는 1 이상의 정수여야 합니다.");
        }
        if (!Number.isInteger(choiceNumber) || choiceNumber <= 0) {
            throw new Error("choiceNumber는 1 이상의 정수여야 합니다.");
        }

        await connectStompIfNeeded();
        subscribeErrorQueueOnce();
        subscribePongQueueOnce();
        const gameSubscribeResult = subscribeGameTopic(String(gameId));
        const sendResult = sendSubmitChoiceCommand(gameId, quizId, choiceNumber);

        setPanel(submitResponseEl, {
            ...sendResult,
            gameSubscription: gameSubscribeResult
        });
        log("10) 정답 제출 전송 완료");
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
            log("11) LEAVE 전송 + WS 연결 해제 완료");
        } else {
            log("11) WS 미연결 상태로 연결해제 단계만 스킵");
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
    snapshotRoomIdInput.addEventListener("input", () => normalizeRoomIdAcrossSections(snapshotRoomIdInput));
    readyRoomIdInput.addEventListener("input", () => normalizeRoomIdAcrossSections(readyRoomIdInput));
    botRoomIdInput.addEventListener("input", () => normalizeRoomIdAcrossSections(botRoomIdInput));
    startRoomIdInput.addEventListener("input", () => normalizeRoomIdAcrossSections(startRoomIdInput));
    startQuizRoomIdInput.addEventListener("input", () => normalizeRoomIdAcrossSections(startQuizRoomIdInput));
    startQuizGameIdInput.addEventListener("input", refreshComputedFields);
    submitGameIdInput.addEventListener("input", refreshComputedFields);
    submitQuizIdInput.addEventListener("input", refreshComputedFields);
    submitChoiceNumberInput.addEventListener("input", refreshComputedFields);
    leaveRoomIdInput.addEventListener("input", () => normalizeRoomIdAcrossSections(leaveRoomIdInput));

    bindAsync(loginBtn, executeGuestLogin);
    bindAsync(loadRoomsBtn, executeLoadRooms);
    bindAsync(createRoomFlowBtn, executeCreateRoomFlow);
    bindAsync(joinFlowBtn, executeJoinFlow);
    bindAsync(snapshotBtn, executeSnapshot);
    bindAsync(readyBtn, async () => executeReadyLike("ready"));
    bindAsync(unreadyBtn, async () => executeReadyLike("unready"));
    bindAsync(botBtn, executeAddBot);
    bindAsync(startBtn, async () => executeStartGame());
    bindAsync(gameLoadBtn, executeGameLoad);
    bindAsync(submitChoiceBtn, executeSubmitChoice);
    bindAsync(leaveDisconnectBtn, executeLeaveAndDisconnect);
    bindAsync(pingBtn, executePing);
    bindAsync(seedRoomsTopBtn, executeSeedRoomsTop);

    clearLogBtn.addEventListener("click", () => {
        logEl.textContent = "";
    });

    window.addEventListener("beforeunload", disconnectStomp);

    initialize();
})();
