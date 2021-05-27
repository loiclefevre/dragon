import dragonlogo from './dragon-logo.png';
import './App.css';
import Background from './components/Background'
import React from 'react'
import styled from 'styled-components'
import { useTable, usePagination } from 'react-table'
import ORDS from './ORDS';

const Styles = styled.div`
  padding: 1rem;
  z-index: 1;
  position: relative;
  background-color: rgba(40,40,40,0.75);
  color: white;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;

  table {
    width: 100%;
    border-spacing: 0;
    border: 1px solid lightgray;

    tr {
      :last-child {
        td {
          border-bottom: 0;
        }
      }

      :hover {
      	background-color: rgba(100, 100, 100, 0.8);
      }

      :nth-child(odd) {
      	background-color: rgba(13, 23, 30, 0.8);
      }
    }

    th,
    td {
      margin: 0;
      padding: 0.5vmin;
      border-bottom: 1px solid lightgray;
      border-right: 1px solid lightgray;

      width: 1%;
      &.collapse {
        width: 0.0000000001%;
      }

      :last-child {
        border-right: 0;
      }
    }
  }

  .pagination {
    padding: 0.5rem;
  }
`

function Table({ columns, data, fetchData,
                                loading,
                                pageCount: controlledPageCount, }) {
  // Use the state and functions returned from useTable to build your UI
  const {
    getTableProps,
    getTableBodyProps,
    headerGroups,
    prepareRow,
    page,
    canPreviousPage,
    canNextPage,
    pageOptions,
    pageCount,
    gotoPage,
    nextPage,
    previousPage,
    setPageSize,
    // Get the state from the instance
    state: { pageIndex, pageSize },
  } = useTable(
    {
      columns,
      data,
      initialState: { pageIndex: 0 }, // Pass our hoisted table state
      manualPagination: true, // Tell the usePagination
      // hook that we'll handle our own data fetching
      // This means we'll also have to provide our own
      // pageCount.
      pageCount: controlledPageCount,
    },
    usePagination
  )

  // Listen for changes in pagination and use the state to fetch our new data
  React.useEffect(() => {
    fetchData({ pageIndex, pageSize })
  }, [fetchData, pageIndex, pageSize])

  // Render the UI for your table
  return (
    <>
      <table {...getTableProps()}>
        <thead>
          {headerGroups.map(headerGroup => (
            <tr {...headerGroup.getHeaderGroupProps()}>
              {headerGroup.headers.map(column => (
                <th {...column.getHeaderProps()}>
                  {column.render('Header')}
                  <span>
                    {column.isSorted
                      ? column.isSortedDesc
                        ? ' 🔽'
                        : ' 🔼'
                      : ''}
                  </span>
                </th>
              ))}
            </tr>
          ))}
        </thead>
        <tbody {...getTableBodyProps()}>
          {page.map((row, i) => {
            prepareRow(row)
            return (
              <tr {...row.getRowProps()}>
                {row.cells.map(cell => {
                  return <td {...cell.getCellProps()}>{cell.render('Cell')}</td>
                })}
              </tr>
            )
          })}
          <tr>
            {loading ? (
              // Use our custom loading state to show a loading indicator
              <td colSpan="10000">Loading...</td>
            ) : (
              <td colSpan="10000">
                Showing {page.length} of ~{controlledPageCount * pageSize}{' '}
                results
              </td>
            )}
          </tr>
        </tbody>
      </table>
      {/*
        Pagination can be built however you'd like.
        This is just a very basic UI implementation:
      */}
      <div className="pagination">
        <button onClick={() => gotoPage(0)} disabled={!canPreviousPage}>
          {'<<'}
        </button>{' '}
        <button onClick={() => previousPage()} disabled={!canPreviousPage}>
          {'<'}
        </button>{' '}
        <button onClick={() => nextPage()} disabled={!canNextPage}>
          {'>'}
        </button>{' '}
        <button onClick={() => gotoPage(pageCount - 1)} disabled={!canNextPage}>
          {'>>'}
        </button>{' '}
        <span>
          Page{' '}
          <strong>
            {pageIndex + 1} of {pageOptions.length}
          </strong>{' '}
        </span>
        <span>
          | Go to page:{' '}
          <input
            type="number"
            defaultValue={pageIndex + 1}
            onChange={e => {
              const page = e.target.value ? Number(e.target.value) - 1 : 0
              gotoPage(page)
            }}
            style={{ width: '100px' }}
          />
        </span>{' '}
        <select
          value={pageSize}
          onChange={e => {
            setPageSize(Number(e.target.value))
          }}
        >
          {[5, 10, 15, 20].map(pageSize => (
            <option key={pageSize} value={pageSize}>
              Show {pageSize}
            </option>
          ))}
        </select>
      </div>
    </>
  )
}

var totalPurchaseOrders = 0;
var tick = 0;
var serverData = [];

function App() {
const columns = React.useMemo(
    () => [
      {
        Header: 'Purchase Order',
        columns: [
          {
            Header: 'ID',
            accessor: 'PONumber',
          },
          {
            Header: 'Reference',
            accessor: 'Reference',
          },
        ],
      },
      {
        Header: 'Details',
        columns: [
          {
            Header: 'Requestor',
            accessor: 'requestor',
          },
          {
            Header: 'Cost Center',
            accessor: 'CostCenter',
          },
        ],
      },
    ],
    []
  )

  // We'll start our table without any data
  const [data, setData] = React.useState([])
  const [loading, setLoading] = React.useState(false)
  const [pageCount, setPageCount] = React.useState(0)
  const fetchIdRef = React.useRef(0)

  const fetchData = React.useCallback(({ pageSize, pageIndex }) => {
    // This will get called when the table needs new data
    // You could fetch your data from literally anywhere,
    // even a server. But for this example, we'll just fake it.

    // Give this fetch an ID
    const fetchId = ++fetchIdRef.current

    // Set the loading state
    setLoading(true)

    // We'll even set a delay to simulate a server here
    setTimeout(async () => {
      // Only update the data if this is the latest fetch
      if (fetchId === fetchIdRef.current) {
        const startRow = pageSize * pageIndex
        //const endRow = startRow + pageSize

        serverData = await ORDS.getPurchaseOrders(startRow, pageSize)

        // Your server could send back total page count.
        tick++;

        if(totalPurchaseOrders === 0 || tick > 5) {
            totalPurchaseOrders = await ORDS.getNumberOfDocumentsInCollection('purchase_orders')
            tick = 0;
        }

        setPageCount(Math.floor(totalPurchaseOrders / pageSize))

        setData(serverData)

        setLoading(false)
      }
    }, 1000)
  }, [])


  return (
    <div className="App">
        <Background />

        <Styles>
            <Table columns={columns} data={data}
                   fetchData={fetchData} loading={loading} pageCount={pageCount}/>
        </Styles>

      <footer className="App-footer">
        <a
          className="App-link"
          href="https://github.com/loiclefevre/dragon"
          target="_blank"
          rel="noopener noreferrer"
          >
          <img src={dragonlogo} className="Dragon-logo" alt="DRAGON Stack logo" title="DRAGON Stack" />
        </a>
        <p className="credits">Background inspired from this <a
              className="App-link"
			  href="https://www.shadertoy.com/view/MdfGRX"
              target="_blank"
              rel="noopener noreferrer"
			  >
			  shader
			</a> by <a
              className="App-link"
			  href="http://iquilezles.org/"
              target="_blank"
              rel="noopener noreferrer"
			  >
			  I&ntilde;igo Quilez
			</a>
		</p>
      </footer>
    </div>
  );
}

export default App;
