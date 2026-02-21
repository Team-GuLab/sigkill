import { Skeleton } from "@/ui/skeleton";
import { MAX_CAPACITY } from "@/constants/room";

export const WaitingRoomSkeleton = () => {
  return (
    <div className="scrollbar-hide flex h-[calc(100vh-8rem)] flex-col p-2 pt-6">
      {/* 방제목 및 방ID 스켈레톤 */}
      <div className="mb-4 flex-none">
        <div className="flex items-center gap-2">
          <div className="flex-1 space-y-2">
            <Skeleton className="h-6 w-40" />
            <Skeleton className="h-4 w-32" />
          </div>
        </div>
      </div>

      {/* 참가자 정보 스켈레톤 */}
      <section className="flex-1">
        <div className="mb-3 space-y-3">
          <Skeleton className="h-5 w-36" />

          {/* 플레이어 슬롯 스켈레톤 */}
          <div className="grid grid-cols-1 gap-3">
            {Array.from({ length: MAX_CAPACITY }).map((_, index) => (
              <div key={index} className="space-y-2">
                <Skeleton className="h-12 rounded-lg" />
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* 하단 버튼 스켈레톤 */}
      <div className="bg-background sticky bottom-0 flex-none">
        <div className="flex h-10 items-center gap-2">
          <Skeleton className="h-10 w-28" />
          <Skeleton className="h-10 flex-1" />
        </div>
      </div>
    </div>
  );
};
