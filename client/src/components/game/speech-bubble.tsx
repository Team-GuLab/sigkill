interface SpeechBubbleProps {
  choiceNumber: number;
}

export default function SpeechBubble({ choiceNumber }: SpeechBubbleProps) {
  return (
    // transform-origin을 bottom으로 설정 → 손잡이 끝을 기준으로 스윙 등장
    <div
      className="animate-sign-pop flex flex-col items-center drop-shadow-md"
      style={{ transformOrigin: "bottom center" }}
    >
      {/* 원형 팻말 */}
      <div className="flex h-8 w-8 items-center justify-center rounded-full bg-white text-sm font-bold text-gray-800">
        {choiceNumber}
      </div>
      {/* 손잡이 */}
      <div className="h-3 w-2.5 rounded-b-sm bg-white" />
    </div>
  );
}
