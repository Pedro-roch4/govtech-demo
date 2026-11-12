import { z } from "zod";

// Espelham as validacoes do back-end (Bean Validation). O servidor continua sendo a fonte da verdade.

export const loginSchema = z.object({
  email: z.email("E-mail inválido"),
  senha: z.string().min(1, "Informe a senha"),
});

export const registroSchema = z.object({
  nome: z.string().min(1, "Informe o nome").max(120, "Máximo de 120 caracteres"),
  email: z.email("E-mail inválido").max(160, "Máximo de 160 caracteres"),
  senha: z.string().min(8, "Mínimo de 8 caracteres").max(72, "Máximo de 72 caracteres"),
});

const texto = (max: number) => z.string().max(max, `Máximo de ${max} caracteres`);

/** Rascunho: tudo opcional (autosave). A completude e exigida pelo servidor ao enviar. */
export const rascunhoSchema = z.object({
  nomeEstabelecimento: texto(160),
  atividade: texto(160),
  endereco: texto(255),
  latitude: z.number("Número inválido").min(-90, "Entre -90 e 90").max(90, "Entre -90 e 90").optional(),
  longitude: z.number("Número inválido").min(-180, "Entre -180 e 180").max(180, "Entre -180 e 180").optional(),
});

export const analiseSchema = z.object({
  criterios: z
    .array(
      z
        .object({
          criterio: z.string(),
          atendido: z.boolean(),
          observacao: texto(500),
        })
        .refine((c) => c.atendido || c.observacao.trim() !== "", {
          path: ["observacao"],
          message: "Explique por que o critério não foi atendido",
        }),
    )
    .length(4),
});

export const indeferirSchema = z.object({
  motivo: z.string().trim().min(1, "Informe o motivo do indeferimento").max(500, "Máximo de 500 caracteres"),
});

export type LoginValores = z.infer<typeof loginSchema>;
export type RegistroValores = z.infer<typeof registroSchema>;
export type RascunhoValores = z.infer<typeof rascunhoSchema>;
export type AnaliseValores = z.infer<typeof analiseSchema>;
export type IndeferirValores = z.infer<typeof indeferirSchema>;
