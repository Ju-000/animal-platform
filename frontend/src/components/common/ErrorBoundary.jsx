import { Children, cloneElement, Component, isValidElement } from "react";

export default class ErrorBoundary extends Component {
  constructor(props) {
    super(props);
    this.state = {
      hasError: false,
      error: null
    };
    this.reset = this.reset.bind(this);
  }

  static getDerivedStateFromError(error) {
    return {
      hasError: true,
      error
    };
  }

  componentDidCatch(error, errorInfo) {
    console.error("ErrorBoundary caught an error", error, errorInfo);
    this.props.onError?.(error, errorInfo);
  }

  reset() {
    this.setState({
      hasError: false,
      error: null
    });
  }

  renderFallback() {
    const { fallback } = this.props;
    const { error } = this.state;
    const isChunkLoadError = error?.name === "ChunkLoadError"
      || /Loading chunk|ChunkLoadError|dynamically imported module/i.test(error?.message ?? "");

    if (isChunkLoadError) {
      return (
        <section className="paw-error-boundary" role="alert">
          <h2>{"업데이트가 있어요. 새로고침 해주세요."}</h2>
          <button type="button" className="paw-action orange" onClick={() => window.location.reload()}>
            {"새로고침"}
          </button>
        </section>
      );
    }

    if (typeof fallback === "function") {
      return fallback({ error, resetErrorBoundary: this.reset });
    }

    if (isValidElement(fallback)) {
      return cloneElement(fallback, {
        error,
        resetErrorBoundary: this.reset
      });
    }

    return fallback ?? null;
  }

  render() {
    if (this.state.hasError) {
      return this.renderFallback();
    }

    return Children.only(this.props.children);
  }
}
