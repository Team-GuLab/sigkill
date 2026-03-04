import { useState } from "react";
import { useRooms } from "@/hooks/room/use-rooms";
import RoomItem from "@/components/room/room-item";
import RoomListPagination from "./room-list-pagination";
import { ItemGroup } from "@/ui/item";
import { EmptyData } from "@/components/common/empty-data";
import { RefreshCcwIcon } from "lucide-react";

const PAGE_SIZE = 6;

// 방 목록의 방 다건
export default function Rooms() {
  const [currentPage, setCurrentPage] = useState(0);
  const [isSpinning, setIsSpinning] = useState(false);

  const { data, refetch } = useRooms({
    page: currentPage,
    size: PAGE_SIZE,
  });

  const handlePageChange = (page: number) => {
    setCurrentPage(page);
  };

  const handleRefresh = () => {
    setIsSpinning(true);
    refetch();
  };

  const { totalPages, rooms } = data;

  return (
    <>
      <section>
        <ItemGroup className="gap-3">
          {rooms.map(room => (
            <RoomItem key={room.roomId} {...room} />
          ))}
        </ItemGroup>
      </section>

      {totalPages === 0 && rooms.length === 0 && (
        <EmptyData
          title="방이 없습니다"
          description="현재 생성된 방이 없습니다. 새로운 방을 만들어보세요."
          buttonText="새로고침"
          buttonIcon={
            <span
              className={isSpinning ? "animate-[spin_0.6s_linear]" : ""}
              onAnimationEnd={() => setIsSpinning(false)}
            >
              <RefreshCcwIcon />
            </span>
          }
          onButtonClick={handleRefresh}
        />
      )}

      {totalPages > 0 && (
        <div className="mt-8">
          <RoomListPagination
            totalPages={totalPages}
            currentPage={currentPage}
            handlePageChange={handlePageChange}
          />
        </div>
      )}
    </>
  );
}
