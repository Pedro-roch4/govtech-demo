"use client";

import { useQuery } from "@tanstack/react-query";
import { api, ApiError } from "./api";
import type { Usuario } from "./tipos";

/** Usuario da sessao, ou null se nao estiver logado. A chamada tambem faz o servidor entregar o cookie CSRF. */
export function useEu() {
  return useQuery({
    queryKey: ["eu"],
    queryFn: async () => {
      try {
        return await api.get<Usuario>("/api/auth/eu");
      } catch (e) {
        if (e instanceof ApiError && e.status === 401) return null;
        throw e;
      }
    },
    retry: false,
    staleTime: 60_000,
  });
}
