import { STATUS, type Status } from "@/lib/tipos";

export default function Selo({ status }: { status: Status }) {
  return <span className={`rounded-full px-2.5 py-0.5 text-xs font-medium ${STATUS[status].cor}`}>{STATUS[status].rotulo}</span>;
}
