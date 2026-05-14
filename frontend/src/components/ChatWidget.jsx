import { useEffect, useRef, useState } from "react";
import { fetchChatQuickAnswer, fetchChatQuickAnswers, sendChatMessage } from "../lib/api";

const initialMessage = {
  role: "assistant",
  content: "안녕하세요! 저는 입양 상담사 포동이에요 🐾 입양을 고려하고 계신가요? 어떤 환경에서 생활하시나요?",
  // This greeting is rendered only in the UI; AI conversations must start with a user message.
  uiOnly: true
};

const fallbackQuickQuestions = [
  { label: "입양 전 체크리스트", keyword: "입양 전 체크리스트" },
  { label: "강아지 식단 가이드", keyword: "강아지 식단 가이드" },
  { label: "산책 꿀팁 알려줘", keyword: "산책 꿀팁 알려줘" }
];

function ChatMessage({ message }) {
  const lines = message.content
    .split("\n")
    .map((line) => line.trim())
    .filter(Boolean);
  const hasChecklist = message.role === "assistant"
    && lines.length > 1
    && lines.some((line) => /^(✅|•|-)/.test(line));

  return (
    <article className={`paw-chat-message ${message.role}`}>
      {message.role === "assistant" ? (
        <span className="paw-chat-avatar" aria-hidden="true">🤖</span>
      ) : null}
      <div className="paw-chat-bubble-content">
        {hasChecklist ? (
          lines.map((line, index) => {
            const cleaned = line.replace(/^(✅|•|-)\s*/, "");
            return (
              <p key={`${cleaned}-${index}`} className="paw-chat-check-line">
                <span aria-hidden="true">✅</span>
                {cleaned}
              </p>
            );
          })
        ) : (
          <p>{message.content}</p>
        )}
      </div>
    </article>
  );
}

export default function ChatWidget() {
  const [open, setOpen] = useState(false);
  const [messages, setMessages] = useState([initialMessage]);
  const [input, setInput] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [quickButtons, setQuickButtons] = useState(fallbackQuickQuestions);
  const [activeQuickKeyword, setActiveQuickKeyword] = useState("");
  const bodyRef = useRef(null);

  useEffect(() => {
    fetchChatQuickAnswers()
      .then((data) => {
        if (Array.isArray(data) && data.length > 0) {
          setQuickButtons(data);
        }
      })
      .catch(() => setQuickButtons(fallbackQuickQuestions));
  }, []);

  useEffect(() => {
    if (!open || !bodyRef.current) return;
    bodyRef.current.scrollTop = bodyRef.current.scrollHeight;
  }, [messages, loading, open]);

  async function sendMessage(content) {
    if (!content || loading) return;

    const nextMessages = [...messages, { role: "user", content, uiOnly: false }];
    setMessages(nextMessages);
    setInput("");
    setError("");
    setLoading(true);

    try {
      const apiMessages = nextMessages
        .filter((message) => !message.uiOnly)
        .map(({ role, content }) => ({ role, content }));
      const payload = await sendChatMessage(apiMessages);
      setMessages((current) => [...current, { role: "assistant", content: payload.content, uiOnly: false }]);
    } catch {
      setError("포동이가 잠깐 자리를 비웠어요 🐾 잠시 후 다시 말을 걸어주세요!");
    } finally {
      setLoading(false);
    }
  }

  async function handleSubmit(event) {
    event.preventDefault();
    await sendMessage(input.trim());
  }

  async function sendQuickAnswer(button) {
    if (!button?.keyword || loading) return;

    setMessages((current) => [...current, { role: "user", content: button.label, uiOnly: true }]);
    setInput("");
    setError("");
    setLoading(true);
    setActiveQuickKeyword(button.keyword);

    try {
      const payload = await fetchChatQuickAnswer(button.keyword);
      setMessages((current) => [...current, { role: "assistant", content: payload.content, uiOnly: true }]);
    } catch {
      setError("포동이가 잠깐 자리를 비웠어요 🐾 잠시 후 다시 말을 걸어주세요!");
    } finally {
      setLoading(false);
      setActiveQuickKeyword("");
    }
  }

  return (
    <div className="paw-chat-widget">
      {open ? (
        <section className="paw-chat-drawer" aria-label="입양 상담 챗봇">
          <header className="paw-chat-head">
            <div className="paw-chat-brand">
              <span className="paw-chat-robot" aria-hidden="true">🤖</span>
              <div>
                <strong>포동이</strong>
                <small>반려생활 AI 상담사</small>
              </div>
            </div>
            <button type="button" onClick={() => setOpen(false)} aria-label="챗봇 닫기">×</button>
          </header>

          <div className="paw-chat-body" ref={bodyRef}>
            {messages.map((message, index) => (
              <ChatMessage key={`${message.role}-${index}`} message={message} />
            ))}
            {loading ? (
              <div className="paw-chat-typing" aria-label="포동이가 입력 중">
                <span />
                <span />
                <span />
              </div>
            ) : null}
          </div>

          {error ? <p className="paw-chat-error">{error}</p> : null}

          <div className="paw-chat-quick-actions" aria-label="빠른 질문">
            {quickButtons.map((button) => (
              <button
                key={button.keyword}
                type="button"
                disabled={loading}
                className={activeQuickKeyword === button.keyword ? "active" : ""}
                onClick={() => sendQuickAnswer(button)}
              >
                {button.label}
              </button>
            ))}
          </div>

          <form className="paw-chat-form" onSubmit={handleSubmit}>
            <input
              value={input}
              onChange={(event) => setInput(event.target.value)}
              placeholder="무엇이든 물어보세요!"
              maxLength={1000}
            />
            <button type="submit" disabled={loading || !input.trim()} aria-label="메시지 보내기">➤</button>
          </form>
        </section>
      ) : null}

      <button type="button" className="paw-chat-bubble" onClick={() => setOpen((current) => !current)} aria-label="입양 상담 챗봇 열기">
        🐾
      </button>
    </div>
  );
}
