import { redirect } from "next/navigation";

export default function Inicio() {
  redirect("/alvaras"); // sem sessao, o layout de /alvaras manda para /login
}
