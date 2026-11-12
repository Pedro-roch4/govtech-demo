"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { api, ApiError } from "@/lib/api";
import {
  loginSchema,
  registroSchema,
  type LoginValores,
  type RegistroValores,
} from "@/lib/schemas";
import type { Usuario } from "@/lib/tipos";
import { botao, campo, cartao, erro, rotulo } from "@/lib/ui";

export default function Login() {
  const [criando, setCriando] = useState(false);
  return (
    <div className="mx-auto max-w-sm">
      <div className={cartao}>
        <h1 className="mb-4 text-lg font-semibold">{criando ? "Criar conta de cidadão" : "Entrar"}</h1>
        {criando ? <Registro aoCriar={() => setCriando(false)} /> : <Entrar />}
        <button className="mt-4 text-sm text-blue-700 hover:underline" onClick={() => setCriando(!criando)}>
          {criando ? "Já tenho conta" : "Criar conta"}
        </button>
      </div>
      {!criando && (
        <p className="mt-4 text-center text-xs text-slate-500">
          Ambiente de demonstração com dados fictícios. Os usuários de teste são criados pelo perfil{" "}
          <code>demo</code> do back-end (veja o README).
        </p>
      )}
    </div>
  );
}

function Entrar() {
  const router = useRouter();
  const qc = useQueryClient();
  const form = useForm<LoginValores>({ resolver: zodResolver(loginSchema) });
  const login = useMutation({
    mutationFn: (v: LoginValores) => api.post<Usuario>("/api/auth/login", v),
    onSuccess: (u) => {
      qc.setQueryData(["eu"], u);
      router.push("/alvaras");
    },
  });
  const { errors } = form.formState;

  return (
    <form onSubmit={form.handleSubmit((v) => login.mutate(v))} className="space-y-3" noValidate>
      <div>
        <label className={rotulo} htmlFor="email">E-mail</label>
        <input id="email" type="email" autoComplete="username" className={campo} {...form.register("email")} />
        {errors.email && <p className={erro}>{errors.email.message}</p>}
      </div>
      <div>
        <label className={rotulo} htmlFor="senha">Senha</label>
        <input id="senha" type="password" autoComplete="current-password" className={campo} {...form.register("senha")} />
        {errors.senha && <p className={erro}>{errors.senha.message}</p>}
      </div>
      {login.error && <p className={erro}>{mensagem(login.error)}</p>}
      <button className={`${botao} w-full`} disabled={login.isPending}>Entrar</button>
    </form>
  );
}

function Registro({ aoCriar }: { aoCriar: () => void }) {
  const form = useForm<RegistroValores>({ resolver: zodResolver(registroSchema) });
  const registrar = useMutation({
    mutationFn: (v: RegistroValores) => api.post("/api/auth/registrar", v),
    onSuccess: aoCriar,
  });
  const { errors } = form.formState;

  return (
    <form onSubmit={form.handleSubmit((v) => registrar.mutate(v))} className="space-y-3" noValidate>
      <div>
        <label className={rotulo} htmlFor="nome">Nome</label>
        <input id="nome" className={campo} {...form.register("nome")} />
        {errors.nome && <p className={erro}>{errors.nome.message}</p>}
      </div>
      <div>
        <label className={rotulo} htmlFor="email">E-mail</label>
        <input id="email" type="email" autoComplete="username" className={campo} {...form.register("email")} />
        {errors.email && <p className={erro}>{errors.email.message}</p>}
      </div>
      <div>
        <label className={rotulo} htmlFor="senha">Senha</label>
        <input id="senha" type="password" autoComplete="new-password" className={campo} {...form.register("senha")} />
        {errors.senha && <p className={erro}>{errors.senha.message}</p>}
      </div>
      {registrar.error && <p className={erro}>{mensagem(registrar.error)}</p>}
      <button className={`${botao} w-full`} disabled={registrar.isPending}>Criar conta</button>
    </form>
  );
}

function mensagem(e: Error) {
  return e instanceof ApiError ? e.message : "Não foi possível falar com o servidor.";
}
