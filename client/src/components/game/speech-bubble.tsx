interface SpeechBubbleProps {
  choiceNumber: number;
}

export default function SpeechBubble({ choiceNumber }: SpeechBubbleProps) {
  return (
    <div className="flex flex-col items-center drop-shadow">
      <div className="rounded-md bg-white px-3 py-1 text-sm font-bold text-gray-800">
        {choiceNumber}
      </div>
      {/* 아래쪽 꼬리 */}
      <div className="h-0 w-0 border-t-8 border-r-6 border-l-6 border-t-white border-r-transparent border-l-transparent" />
    </div>
  );
}
