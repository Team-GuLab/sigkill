(function () {
    "use strict";

    const serverBaseInput = document.getElementById("serverBase");
    const wsUrlPreviewInput = document.getElementById("wsUrlPreview");

    const loginBtn = document.getElementById("loginBtn");
    const loadRoomsBtn = document.getElementById("loadRoomsBtn");
    const joinFlowBtn = document.getElementById("joinFlowBtn");
    const readyBtn = document.getElementById("readyBtn");
    const unreadyBtn = document.getElementById("unreadyBtn");
    const leaveDisconnectBtn = document.getElementById("leaveDisconnectBtn");
    const pingBtn = document.getElementById("pingBtn");
    const clearLogBtn = document.getElementById("clearLogBtn");
    const seedRoomsTopBtn = document.getElementById("seedRoomsTopBtn");

    const roomsPageInput = document.getElementById("roomsPage");
    const roomsSizeInput = document.getElementById("roomsSize");

    const joinRoomIdInput = document.getElementById("joinRoomId");
    const readyRoomIdInput = document.getElementById("readyRoomId");
    const leaveRoomIdInput = document.getElementById("leaveRoomId");

    const roomTopicPreviewInput = document.getElementById("roomTopicPreview");
    const errorTopicPreviewInput = document.getElementById("errorTopicPreview");
    const readyPayloadPreviewInput = document.getElementById("readyPayloadPreview");
    const leavePayloadPreviewInput = document.getElementById("leavePayloadPreview");

    const loginResponseEl = document.getElementById("loginResponse");
    const roomsResponseEl = document.getElementById("roomsResponse");
    const joinRequestEl = document.getElementById("joinRequest");
    const joinResponseEl = document.getElementById("joinResponse");
    const joinSendEl = document.getElementById("joinSend");
    const readyResponseEl = document.getElementById("readyResponse");
    const leaveResponseEl = document.getElementById("leaveResponse");

    const roomEventPanelEl = document.getElementById("roomEventPanel");
    const roomSnapshotPanelEl = document.getElementById("roomSnapshotPanel");
    const errorEventPanelEl = document.getElementById("errorEventPanel");
    const pongEventPanelEl = document.getElementById("pongEventPanel");
    const logEl = document.getElementById("log");

    const state = {
        stompClient: null,
        roomSubscription: null,
        roomSnapshotSubscription: null,
        errorSubscription: null,
        pongSubscription: null,
        subscribedRoomId: null
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

    function subscribeRoomSnapshotQueueOnce() {
        if (state.roomSnapshotSubscription) {
            return {subscribed: true, newlySubscribed: false};
        }

        state.roomSnapshotSubscription = state.stompClient.subscribe("/user/queue/room/snapshot", (frame) => {
            const parsed = parseJsonSafe(frame.body);
            setPanel(roomSnapshotPanelEl, parsed || frame.body);
            log("ROOM SNAPSHOT 이벤트 수신");
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
        });

        state.subscribedRoomId = roomId;
        return {subscribed: true, topic: "/topic/room/" + roomId};
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
        subscribeRoomSnapshotQueueOnce();
        const sendResult = sendRoomCommand("join", roomId);
        subscribeRoomTopic(roomId);
        setPanel(joinSendEl, sendResult);
        log("3) 방 참가 통합 실행 완료");
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
        log("4) " + command.toUpperCase() + " 전송 완료");
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
        if (state.roomSnapshotSubscription) {
            state.roomSnapshotSubscription.unsubscribe();
            state.roomSnapshotSubscription = null;
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
    }

    async function executeLeaveAndDisconnect() {
        const roomId = getValueOrThrow(leaveRoomIdInput, "roomId");
        normalizeRoomIdAcrossSections(leaveRoomIdInput);

        if (state.stompClient && state.stompClient.connected) {
            const sendResult = sendRoomCommand("leave", roomId);
            setPanel(leaveResponseEl, sendResult);
            await new Promise((resolve) => setTimeout(resolve, 150));
            disconnectStomp();
            log("5) LEAVE 전송 + WS 연결 해제 완료");
        } else {
            log("5) WS 미연결 상태로 연결해제 단계만 스킵");
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
    leaveRoomIdInput.addEventListener("input", () => normalizeRoomIdAcrossSections(leaveRoomIdInput));

    bindAsync(loginBtn, executeGuestLogin);
    bindAsync(loadRoomsBtn, executeLoadRooms);
    bindAsync(joinFlowBtn, executeJoinFlow);
    bindAsync(readyBtn, async () => executeReadyLike("ready"));
    bindAsync(unreadyBtn, async () => executeReadyLike("unready"));
    bindAsync(leaveDisconnectBtn, executeLeaveAndDisconnect);
    bindAsync(pingBtn, executePing);
    bindAsync(seedRoomsTopBtn, executeSeedRoomsTop);

    clearLogBtn.addEventListener("click", () => {
        logEl.textContent = "";
    });

    window.addEventListener("beforeunload", disconnectStomp);

    initialize();
})();
