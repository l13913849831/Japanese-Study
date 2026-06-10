import { useEffect, useState } from "react";
import { hasMobileToken } from "@/shared/auth/token-store";
import { relaunchLogin } from "@/shared/routes";

export function useAuthGuard() {
  const [authenticated] = useState(hasMobileToken);

  useEffect(() => {
    if (!authenticated) {
      void relaunchLogin();
    }
  }, [authenticated]);

  return authenticated;
}
