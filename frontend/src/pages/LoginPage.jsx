import { useState } from "react";
import AuthModal from "../components/AuthModal";

export default function LoginPage() {
  const [open, setOpen] = useState(true);

  return <AuthModal open={open} onClose={() => setOpen(false)} />;
}