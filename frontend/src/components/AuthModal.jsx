import { useEffect, useMemo, useState } from "react";
import {
  checkNickname,
  checkUsername,
  fetchSocialProviders,
  loginWithPassword,
  signUpWithPassword
} from "../lib/api";
import { getDevVerificationCode, isFirebaseConfigured } from "../lib/localAuth";

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080";

const SOCIAL_PROVIDER_BASE = [
  { id: "google", name: "구글", brandClass: "google" },
  { id: "kakao", name: "카카오", brandClass: "kakao" },
  { id: "naver", name: "네이버", brandClass: "naver" }
];

const POSTCODE_SCRIPT_URL = "//t1.daumcdn.net/mapjsapi/bundle/postcode/prod/postcode.v2.js";

function loadPostcodeScript() {
  if (window.daum?.Postcode) {
    return Promise.resolve();
  }

  return new Promise((resolve, reject) => {
    const existingScript = document.querySelector(`script[src="${POSTCODE_SCRIPT_URL}"]`);
    if (existingScript) {
      existingScript.addEventListener("load", resolve, { once: true });
      existingScript.addEventListener("error", reject, { once: true });
      return;
    }

    const script = document.createElement("script");
    script.src = POSTCODE_SCRIPT_URL;
    script.async = true;
    script.onload = resolve;
    script.onerror = reject;
    document.head.appendChild(script);
  });
}

export default function AuthModal({ open, onClose, onSuccess }) {
  const [mode, setMode] = useState("login");
  const [providers, setProviders] = useState([]);
  const [message, setMessage] = useState("");
  const [loginForm, setLoginForm] = useState({ username: "", password: "" });
  const [signupForm, setSignupForm] = useState({
    username: "",
    password: "",
    passwordConfirm: "",
    name: "",
    phone: "",
    code: "",
    email: "",
    birthDate: "",
    gender: "",
    address: "",
    addressDetail: "",
    privacyConsent: false
  });
  const [verificationSent, setVerificationSent] = useState(false);
  const [verificationConfirmed, setVerificationConfirmed] = useState(false);
  const [usernameChecked, setUsernameChecked] = useState(false);
  const [nicknameChecked, setNicknameChecked] = useState(false);
  const [usernameMessage, setUsernameMessage] = useState("");
  const [usernameAvailable, setUsernameAvailable] = useState(false);
  const [nicknameMessage, setNicknameMessage] = useState("");
  const [nicknameAvailable, setNicknameAvailable] = useState(false);
  const [verificationMessage, setVerificationMessage] = useState("");
  const [verificationAvailable, setVerificationAvailable] = useState(false);

  useEffect(() => {
    if (!open) return;
    fetchSocialProviders()
      .then((data) => setProviders(data ?? []))
      .catch(() => setProviders([]));
  }, [open]);

  const socialProviders = useMemo(() => {
    const providerMap = new Map((providers ?? []).map((provider) => [provider.id, provider]));
    return SOCIAL_PROVIDER_BASE.map((base) => {
      const found = providerMap.get(base.id);
      return {
        ...base,
        enabled: found?.enabled ?? false,
        authorizationUrl: found?.authorizationUrl ?? null
      };
    });
  }, [providers]);

  if (!open) return null;

  function switchMode(nextMode) {
    setMode(nextMode);
    setMessage("");
    setUsernameMessage("");
    setNicknameMessage("");
    setVerificationMessage("");
  }

  async function handleLoginSubmit(event) {
    event.preventDefault();
    try {
      const profile = await loginWithPassword(loginForm);
      setMessage("로그인되었습니다.");
      onSuccess?.(profile);
      onClose?.();
    } catch {
      setMessage("아이디 또는 비밀번호를 확인해주세요.");
    }
  }

  async function handleUsernameCheck() {
    try {
      if (!signupForm.username.trim()) {
        setUsernameChecked(false);
        setUsernameAvailable(false);
        setUsernameMessage("❌ 아이디를 입력해주세요.");
        return;
      }

      const result = await checkUsername(signupForm.username);
      if (!result?.available) {
        setUsernameChecked(false);
        setUsernameAvailable(false);
        setUsernameMessage("❌ 이미 사용 중인 아이디입니다.");
        return;
      }

      setUsernameChecked(true);
      setUsernameAvailable(true);
      setUsernameMessage("✅ 사용 가능한 아이디입니다.");
    } catch {
      setUsernameChecked(false);
      setUsernameAvailable(false);
      setUsernameMessage("❌ 아이디 중복확인에 실패했습니다.");
    }
  }

  async function handleNicknameCheck() {
    try {
      if (!signupForm.name.trim()) {
        setNicknameChecked(false);
        setNicknameAvailable(false);
        setNicknameMessage("❌ 닉네임을 입력해주세요.");
        return;
      }

      const result = await checkNickname(signupForm.name);
      if (!result?.available) {
        setNicknameChecked(false);
        setNicknameAvailable(false);
        setNicknameMessage("❌ 이미 사용 중인 닉네임입니다.");
        return;
      }

      setNicknameChecked(true);
      setNicknameAvailable(true);
      setNicknameMessage("✅ 사용 가능한 닉네임입니다.");
    } catch {
      setNicknameChecked(false);
      setNicknameAvailable(false);
      setNicknameMessage("❌ 닉네임 중복확인에 실패했습니다.");
    }
  }

  function handleSendCode() {
    if (!signupForm.phone.trim()) {
      setVerificationAvailable(false);
      setVerificationMessage("❌ 휴대전화 번호를 입력해주세요.");
      return;
    }

    if (isFirebaseConfigured()) {
      setVerificationMessage("인증번호를 전송했습니다.");
    } else {
      setVerificationMessage(`개발용 인증번호는 ${getDevVerificationCode()} 입니다.`);
    }

    setVerificationSent(true);
    setVerificationConfirmed(false);
    setVerificationAvailable(false);
  }

  function handleVerifyCode() {
    if (!verificationSent) {
      setVerificationAvailable(false);
      setVerificationMessage("❌ 먼저 인증요청을 해주세요.");
      return;
    }

    const expectedCode = isFirebaseConfigured() ? signupForm.code : getDevVerificationCode();
    if (signupForm.code !== expectedCode) {
      setVerificationAvailable(false);
      setVerificationMessage("❌ 인증번호가 올바르지 않습니다.");
      return;
    }

    setVerificationConfirmed(true);
    setVerificationAvailable(true);
    setVerificationMessage("✅ 인증되었습니다.");
  }

  async function handleAddressSearch() {
    try {
      await loadPostcodeScript();
      new window.daum.Postcode({
        oncomplete: (data) => {
          const baseAddress = data.userSelectedType === "R" ? data.roadAddress : data.jibunAddress;
          const extraAddressParts = [];

          if (data.userSelectedType === "R" && data.bname && /[동|로|가]$/g.test(data.bname)) {
            extraAddressParts.push(data.bname);
          }

          if (data.userSelectedType === "R" && data.buildingName && data.apartment === "Y") {
            extraAddressParts.push(data.buildingName);
          }

          const extraAddress = extraAddressParts.length ? ` (${extraAddressParts.join(", ")})` : "";
          setSignupForm((current) => ({
            ...current,
            address: `${baseAddress}${extraAddress}`
          }));
        }
      }).open();
    } catch {
      setVerificationAvailable(false);
      setVerificationMessage("❌ 주소검색을 불러오지 못했습니다. 잠시 후 다시 시도해주세요.");
    }
  }

  async function handleSignupSubmit(event) {
    event.preventDefault();

    if (!usernameChecked) {
      setUsernameAvailable(false);
      setUsernameMessage("❌ 아이디 중복확인을 해주세요.");
      return;
    }

    if (!nicknameChecked) {
      setNicknameAvailable(false);
      setNicknameMessage("❌ 닉네임 중복확인을 해주세요.");
      return;
    }

    if (signupForm.password !== signupForm.passwordConfirm) {
      setVerificationAvailable(false);
      setVerificationMessage("❌ 비밀번호 확인이 일치하지 않습니다.");
      return;
    }

    if (!verificationSent || !verificationConfirmed) {
      setVerificationAvailable(false);
      setVerificationMessage("❌ 휴대전화 인증을 완료해주세요.");
      return;
    }

    if (!signupForm.email.trim()) {
      setVerificationAvailable(false);
      setVerificationMessage("❌ 이메일을 입력해주세요.");
      return;
    }

    if (!signupForm.privacyConsent) {
      setVerificationAvailable(false);
      setVerificationMessage("❌ 개인정보 수집 및 이용에 동의해주세요.");
      return;
    }

    try {
      const fullAddress = [signupForm.address, signupForm.addressDetail].map((value) => value.trim()).filter(Boolean).join(" ");
      await signUpWithPassword({
        username: signupForm.username,
        password: signupForm.password,
        name: signupForm.name,
        phone: signupForm.phone,
        email: signupForm.email,
        birthDate: signupForm.birthDate,
        gender: signupForm.gender,
        address: fullAddress,
        privacyConsent: signupForm.privacyConsent
      });
      const profile = await loginWithPassword({
        username: signupForm.username,
        password: signupForm.password
      });
      setMessage("회원가입이 완료되었습니다.");
      onSuccess?.(profile);
      onClose?.();
    } catch (error) {
      setVerificationAvailable(false);
      setVerificationMessage(`❌ ${error.message || "회원가입에 실패했습니다. 입력값을 확인해주세요."}`);
    }
  }

  return (
    <div className="auth-modal-backdrop" onClick={onClose}>
      <section className="auth-modal" onClick={(event) => event.stopPropagation()}>
        <div className="auth-modal-head">
          <div>
            <p className="eyebrow">MEMBER</p>
            <h2>{mode === "login" ? "로그인" : "회원가입"}</h2>
          </div>
          <button type="button" className="auth-close" onClick={onClose} aria-label="닫기">
            ×
          </button>
        </div>

        <div className="auth-switcher">
          <button type="button" className={mode === "login" ? "active" : ""} onClick={() => switchMode("login")}>
            로그인
          </button>
          <button type="button" className={mode === "signup" ? "active" : ""} onClick={() => switchMode("signup")}>
            회원가입
          </button>
        </div>

        {mode === "login" ? (
          <form className="auth-form" onSubmit={handleLoginSubmit}>
            <label>
              <span>아이디</span>
              <input
                value={loginForm.username}
                onChange={(event) => setLoginForm((current) => ({ ...current, username: event.target.value }))}
                placeholder="아이디 입력"
              />
            </label>
            <label>
              <span>비밀번호</span>
              <input
                type="password"
                value={loginForm.password}
                onChange={(event) => setLoginForm((current) => ({ ...current, password: event.target.value }))}
                placeholder="비밀번호 입력"
              />
            </label>
            <button type="submit" className="auth-submit">
              로그인
            </button>
          </form>
        ) : (
          <form className="auth-form" onSubmit={handleSignupSubmit}>
            <div className="auth-field-block">
              <div className="auth-code-row">
                <label>
                  <span>아이디</span>
                  <input
                    value={signupForm.username}
                    onChange={(event) => {
                      setSignupForm((current) => ({ ...current, username: event.target.value }));
                      setUsernameChecked(false);
                      setUsernameAvailable(false);
                      setUsernameMessage("");
                    }}
                    placeholder="아이디 입력"
                  />
                </label>
                <button type="button" className="auth-code-button" onClick={handleUsernameCheck}>
                  중복확인
                </button>
              </div>
              {usernameMessage ? (
                <p className={`auth-field-message ${usernameAvailable ? "success" : "error"}`}>{usernameMessage}</p>
              ) : null}
            </div>

            <label>
              <span>비밀번호</span>
              <input
                type="password"
                value={signupForm.password}
                onChange={(event) => setSignupForm((current) => ({ ...current, password: event.target.value }))}
                placeholder="비밀번호 입력"
              />
            </label>

            <label>
              <span>비밀번호 확인</span>
              <input
                type="password"
                value={signupForm.passwordConfirm}
                onChange={(event) => setSignupForm((current) => ({ ...current, passwordConfirm: event.target.value }))}
                placeholder="비밀번호 다시 입력"
              />
            </label>

            <div className="auth-field-block">
              <div className="auth-code-row">
                <label>
                  <span>닉네임</span>
                  <input
                    value={signupForm.name}
                    onChange={(event) => {
                      setSignupForm((current) => ({ ...current, name: event.target.value }));
                      setNicknameChecked(false);
                      setNicknameAvailable(false);
                      setNicknameMessage("");
                    }}
                    placeholder="닉네임 입력"
                  />
                </label>
                <button type="button" className="auth-code-button" onClick={handleNicknameCheck}>
                  중복확인
                </button>
              </div>
              {nicknameMessage ? (
                <p className={`auth-field-message ${nicknameAvailable ? "success" : "error"}`}>{nicknameMessage}</p>
              ) : null}
            </div>

            <label>
              <span>휴대전화 번호</span>
              <input
                value={signupForm.phone}
                onChange={(event) => {
                  setSignupForm((current) => ({ ...current, phone: event.target.value }));
                  setVerificationSent(false);
                  setVerificationConfirmed(false);
                  setVerificationAvailable(false);
                  setVerificationMessage("");
                }}
                placeholder="01012345678"
              />
            </label>

            <label>
              <span>이메일</span>
              <input
                type="email"
                value={signupForm.email}
                onChange={(event) => setSignupForm((current) => ({ ...current, email: event.target.value }))}
                placeholder="example@email.com"
              />
            </label>

            <div className="auth-personal-grid">
              <label>
                <span>생년월일</span>
                <input
                  inputMode="numeric"
                  maxLength={8}
                  value={signupForm.birthDate}
                  onChange={(event) =>
                    setSignupForm((current) => ({
                      ...current,
                      birthDate: event.target.value.replace(/\D/g, "").slice(0, 8)
                    }))
                  }
                  placeholder="YYYYMMDD"
                />
              </label>
              <label>
                <span>성별</span>
                <select
                  value={signupForm.gender}
                  onChange={(event) => setSignupForm((current) => ({ ...current, gender: event.target.value }))}
                >
                  <option value="">선택 안 함</option>
                  <option value="FEMALE">여성</option>
                  <option value="MALE">남성</option>
                  <option value="OTHER">기타</option>
                </select>
              </label>
            </div>

            <div className="auth-field-block">
              <div className="auth-code-row">
                <label>
                  <span>주소</span>
                  <input
                    value={signupForm.address}
                    onChange={(event) => setSignupForm((current) => ({ ...current, address: event.target.value }))}
                    placeholder="주소검색으로 입력"
                    readOnly
                  />
                </label>
                <button type="button" className="auth-code-button" onClick={handleAddressSearch}>
                  주소검색
                </button>
              </div>
            </div>

            <label>
              <span>상세주소</span>
              <input
                value={signupForm.addressDetail}
                onChange={(event) => setSignupForm((current) => ({ ...current, addressDetail: event.target.value }))}
                placeholder="동/호수 등 상세주소"
              />
            </label>

            <div className="auth-code-row">
              <label>
                <span>인증번호</span>
                <input
                  value={signupForm.code}
                  onChange={(event) => {
                    setSignupForm((current) => ({ ...current, code: event.target.value }));
                    setVerificationConfirmed(false);
                    setVerificationAvailable(false);
                    setVerificationMessage("");
                  }}
                  placeholder="인증번호 입력"
                />
              </label>
              <button type="button" className="auth-code-button" onClick={handleSendCode}>
                인증요청
              </button>
            </div>
            {verificationMessage ? (
              <p className={`auth-field-message ${verificationAvailable ? "success" : "error"}`}>{verificationMessage}</p>
            ) : null}

            <button type="button" className="auth-code-confirm" onClick={handleVerifyCode}>
              인증확인
            </button>

            <label className="auth-consent-row">
              <input
                type="checkbox"
                checked={signupForm.privacyConsent}
                onChange={(event) =>
                  setSignupForm((current) => ({ ...current, privacyConsent: event.target.checked }))
                }
              />
              <span>
                개인정보 수집 및 이용에 동의합니다.
                <small>회원 식별, 연락, 입양 상담 진행을 위해 입력한 정보를 저장합니다.</small>
              </span>
            </label>

            <button type="submit" className="auth-submit">
              회원가입
            </button>
          </form>
        )}

        <div className="auth-social-block">
          <div className="auth-social-head">
            <strong>소셜 로그인</strong>
            <span>구글 · 카카오 · 네이버 계정으로 빠르게 시작할 수 있어요.</span>
          </div>
          <div className="auth-social-list">
            {socialProviders.map((provider) =>
              provider.enabled && provider.authorizationUrl ? (
                <a
                  key={provider.id}
                  className={`auth-social-button ${provider.brandClass}`}
                  href={`${API_BASE_URL}${provider.authorizationUrl}`}
                >
                  {provider.name}로 로그인
                </a>
              ) : (
                <button
                  key={provider.id}
                  type="button"
                  className={`auth-social-button ${provider.brandClass} disabled`}
                  disabled
                >
                  {provider.name} 준비중
                </button>
              ),
            )}
          </div>
        </div>

        {mode === "login" && message ? <p className="auth-message">{message}</p> : null}
      </section>
    </div>
  );
}
