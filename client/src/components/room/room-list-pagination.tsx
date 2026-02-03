import {
  Pagination,
  PaginationContent,
  PaginationItem,
  PaginationLink,
  PaginationPrevious,
  PaginationNext,
  PaginationEllipsis,
} from "@/ui/pagination";

// 방 목록 페이지네이션
interface RoomListPaginationProps {
  totalPages: number;
  currentPage: number;
  handlePageChange: (page: number) => void;
}
export default function RoomListPagination({
  totalPages,
  currentPage,
  handlePageChange,
}: RoomListPaginationProps) {
  // 페이지네이션 범위 계산
  const getPageNumbers = () => {
    const pages: (number | "ellipsis")[] = [];
    const maxVisible = 5;

    if (totalPages <= maxVisible) {
      return Array.from({ length: totalPages }, (_, i) => i);
    }

    // 첫 페이지
    pages.push(0);

    if (currentPage > 2) {
      pages.push("ellipsis");
    }

    // 현재 페이지 주변
    const start = Math.max(1, currentPage - 1);
    const end = Math.min(totalPages - 2, currentPage + 1);

    for (let i = start; i <= end; i++) {
      pages.push(i);
    }

    if (currentPage < totalPages - 3) {
      pages.push("ellipsis");
    }

    // 마지막 페이지
    if (totalPages > 1) {
      pages.push(totalPages - 1);
    }

    return pages;
  };

  return (
    <Pagination>
      <PaginationContent>
        <PaginationItem>
          <PaginationPrevious
            onClick={() => currentPage > 0 && handlePageChange(currentPage - 1)}
            className={
              currentPage === 0
                ? "pointer-events-none opacity-50"
                : "cursor-pointer"
            }
          />
        </PaginationItem>

        {getPageNumbers().map((page, index) =>
          page === "ellipsis" ? (
            <PaginationItem key={`ellipsis-${index}`}>
              <PaginationEllipsis />
            </PaginationItem>
          ) : (
            <PaginationItem key={page}>
              <PaginationLink
                onClick={() => handlePageChange(page)}
                isActive={currentPage === page}
                className="cursor-pointer"
              >
                {page + 1}
              </PaginationLink>
            </PaginationItem>
          ),
        )}

        <PaginationItem>
          <PaginationNext
            onClick={() =>
              currentPage < totalPages - 1 && handlePageChange(currentPage + 1)
            }
            className={
              currentPage === totalPages - 1
                ? "pointer-events-none opacity-50"
                : "cursor-pointer"
            }
          />
        </PaginationItem>
      </PaginationContent>
    </Pagination>
  );
}
