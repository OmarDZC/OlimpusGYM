"use strict";

/**
 * Doble de un proveedor compatible con OpenAI, solo para el test end-to-end del asistente
 * (e2e/tests/assistant-booking.spec.ts). No entiende lenguaje natural: sigue siempre el mismo
 * guion de 3 pasos (listar_clases -> reservar_clase -> proponer), y elige la clase buscando
 * cuál de los nombres que devolvió listar_clases aparece tal cual en el primer mensaje del
 * socio (el test escribe el nombre exacto de la clase de prueba que creó).
 * Nunca se usa fuera de `docker-compose.e2e.yml`: el resto del proyecto habla con un LLM real
 * o, en los tests unitarios de ai-service, con un cliente falso en memoria (sin red).
 */
const http = require("node:http");

const PORT = process.env.PORT || 4100;

function readBody(req) {
  return new Promise((resolve) => {
    let data = "";
    req.on("data", (chunk) => (data += chunk));
    req.on("end", () => resolve(data));
  });
}

function chatCompletion(message) {
  return {
    id: "chatcmpl-fake",
    object: "chat.completion",
    created: Math.floor(Date.now() / 1000),
    model: "fake-model",
    choices: [{ index: 0, finish_reason: message.tool_calls ? "tool_calls" : "stop", message }],
    usage: { prompt_tokens: 0, completion_tokens: 0, total_tokens: 0 },
  };
}

function toolCall(name, args) {
  return chatCompletion({
    role: "assistant",
    content: null,
    tool_calls: [{ id: "call_1", type: "function", function: { name, arguments: JSON.stringify(args) } }],
  });
}

function text(content) {
  return chatCompletion({ role: "assistant", content, tool_calls: null });
}

function pickSession(sessions, messages) {
  const firstUser = messages.find((m) => m.role === "user");
  const content = (firstUser && firstUser.content) || "";
  return sessions.find((s) => s.name && content.includes(s.name)) ?? sessions[0] ?? null;
}

function handleChat(body) {
  const messages = body.messages || [];
  const toolResults = messages.filter((m) => m.role === "tool");

  if (toolResults.length === 0) {
    return toolCall("listar_clases", {});
  }

  if (toolResults.length === 1) {
    let sessions = [];
    try {
      sessions = JSON.parse(toolResults[0].content);
    } catch {
      sessions = [];
    }
    const target = pickSession(sessions, messages);
    if (!target) {
      return text("No he encontrado esa clase en el horario.");
    }
    return toolCall("reservar_clase", { session_id: target.id });
  }

  return text("Voy a reservar esa clase, ¿confirmas?");
}

const server = http.createServer(async (req, res) => {
  if (req.method !== "POST" || !req.url.endsWith("/chat/completions")) {
    res.writeHead(404, { "Content-Type": "application/json" }).end(JSON.stringify({ error: "not found" }));
    return;
  }
  let body;
  try {
    body = JSON.parse(await readBody(req));
  } catch {
    res.writeHead(400, { "Content-Type": "application/json" }).end(JSON.stringify({ error: "invalid json" }));
    return;
  }
  const payload = handleChat(body);
  res.writeHead(200, { "Content-Type": "application/json" });
  res.end(JSON.stringify(payload));
});

server.listen(PORT, () => {
  console.log(`fake-llm escuchando en :${PORT}`);
});
