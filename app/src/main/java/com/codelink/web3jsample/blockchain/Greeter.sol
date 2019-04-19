pragma solidity ^0.5.7;

contract Mortal {
  address owner;

  constructor() public {owner = msg.sender;}

  function kill() public {
    if (msg.sender == owner)
      selfdestruct(address(uint160(owner)));
  }
}

contract Greeter is Mortal {
  string greeting;

  constructor(string memory _greeting) public {
    greeting = _greeting;
  }

  function changeGreeting(string memory _greeting) public {
    greeting = _greeting;
  }

  function greet() public view returns (string memory) {
    return greeting;
  }

  function getOwner() public view returns (address) {
    return owner;
  }

  function getBalance() public view returns (uint256) {
    return address(this).balance;
  }
}
